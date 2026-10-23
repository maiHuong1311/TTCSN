"""Chroma RAG. Only product/policy documents persist; never customer queries or measurements."""
from collections import OrderedDict
import hashlib
import json
import math
import re
import threading
import unicodedata

import chromadb
from chromadb.config import Settings as ChromaSettings
from langchain_core.documents import Document


def normalize(text: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFD", text.lower().replace("đ", "d")) if unicodedata.category(c) != "Mn")


def redact(text: str) -> str:
    text = re.sub(r"[\w.+-]+@[\w.-]+\.[a-zA-Z]{2,}", "[email]", text)
    return re.sub(r"(?<!\d)(?:\+84|0)[\d .-]{8,13}\d(?!\d)", "[điện thoại]", text)


def lexical_embedding(text: str) -> list[float]:
    words = re.findall(r"[a-z0-9]+", normalize(text))
    features = words + [a + "_" + b for a, b in zip(words, words[1:])]
    vector = [0.0] * 768
    for word in features:
        index = int.from_bytes(hashlib.sha256(word.encode()).digest()[:4], "little") % 768
        vector[index] += 1.0
    norm = math.sqrt(sum(x * x for x in vector)) or 1
    return [x / norm for x in vector]


class Retriever:
    def __init__(self, settings, http_client):
        self.settings, self.http = settings, http_client
        self.client = chromadb.PersistentClient(path=str(settings.data_dir), settings=ChromaSettings(anonymized_telemetry=False))
        self.lock = threading.RLock()
        self.cache = OrderedDict()

    def embeddings(self, texts: list[str], remote: bool):
        if not remote:
            return [lexical_embedding(t) for t in texts]
        model = self.settings.embedding_model
        response = self.http.post(
            f"{self.settings.google_url}/models/{model}:batchEmbedContents",
            headers={"x-goog-api-key": self.settings.gemini_key},
            json={"requests": [{"model": "models/" + model, "content": {"parts": [{"text": t}]}, "outputDimensionality": 768} for t in texts]},
            timeout=6,
        )
        response.raise_for_status()
        vectors = [item["values"] for item in response.json()["embeddings"]]
        if len(vectors) != len(texts) or any(len(v) != 768 for v in vectors):
            raise ValueError("Unexpected embedding dimensions")
        return vectors

    def retrieve(self, request):
        products = [p for p in request.catalog if p.available]
        docs = [Document(page_content=json.dumps({"name": p.productName, "description": (p.description or "")[:1500],
                   "material": p.material, "price": p.effective_price, "colors": list({v.color for v in p.variants if v.stockQuantity > 0})}, ensure_ascii=False),
                 metadata={"id": "product_" + str(p.productId), "productId": p.productId, "kind": "product"}) for p in products]
        info = json.loads((self.settings.knowledge_dir / "store-info.json").read_text(encoding="utf-8-sig"))
        docs += [Document(page_content=p["title"] + ": " + " ".join(p["paragraphs"]), metadata={"id": "policy_" + p["id"], "kind": "policy"}) for p in info["policies"]]
        message = redact(request.message)
        remote = bool(self.settings.gemini_key) and self.settings.embedding_mode == "gemini"
        # Do not mix model embeddings and lexical vectors in the same collection.
        try:
            found = self._query(docs, message, remote)
        except Exception:
            if not remote:
                raise
            found = self._query(docs, message, False)
        by_id = {p.productId: p for p in products}
        ordered = list(dict.fromkeys(d.metadata.get("productId") for d in found if d.metadata.get("kind") == "product"))
        # Always include only live products from this request, never stale vector metadata.
        return {"message": message, "products": [by_id[i] for i in ordered if i in by_id], "policies": [d.page_content for d in found if d.metadata.get("kind") == "policy"]}

    def _query(self, docs, message, remote):
        model_key = self.settings.embedding_model if remote else "lexical"
        name = "forme_" + hashlib.sha256(model_key.encode()).hexdigest()[:12]
        with self.lock:
            collection = self.client.get_or_create_collection(name=name, embedding_function=None, metadata={"hnsw:space": "cosine"})
            existing = collection.get(include=["metadatas"])
            fingerprints = dict(zip(existing["ids"], existing["metadatas"]))
            changed = [d for d in docs if (fingerprints.get(d.metadata["id"]) or {}).get("hash") != hashlib.sha256(d.page_content.encode()).hexdigest()]
            for start in range(0, len(changed), 50):
                batch = changed[start:start + 50]
                vectors = self.embeddings([d.page_content for d in batch], remote)
                collection.upsert(ids=[d.metadata["id"] for d in batch], documents=[d.page_content for d in batch], embeddings=vectors,
                    metadatas=[{**d.metadata, "hash": hashlib.sha256(d.page_content.encode()).hexdigest()} for d in batch])
            # Query only current document IDs; archived/deleted/out-of-stock items cannot return.
            ids = {d.metadata["id"] for d in docs}
            stale = [i for i in existing["ids"] if i not in ids]
            if stale:
                collection.delete(ids=stale)
            key = model_key + hashlib.sha256(message.encode()).hexdigest()
            vector = self.cache.get(key)
            if vector is None:
                vector = self.embeddings([message], remote)[0]
                self.cache[key] = vector
                while len(self.cache) > 128:
                    self.cache.popitem(last=False)
            result = collection.query(query_embeddings=[vector], n_results=min(12, len(docs)), include=["documents", "metadatas"])
            return [Document(page_content=text, metadata=meta) for text, meta in zip(result["documents"][0], result["metadatas"][0])]
