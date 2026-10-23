import json
from pathlib import Path
import httpx
import pytest
from fastapi.testclient import TestClient
from forme_ai.app import create_app
from forme_ai.engine import Engine, budget
from forme_ai.models import AdviseRequest
from forme_ai.settings import Settings

KNOWLEDGE = Path(__file__).resolve().parents[2] / "src/main/resources/fashion"

def product(id=1, stock=3, price=390000):
    return {"productId": id, "categoryId": 1, "productName": "Áo sơ mi công sở", "description": "Linen thoáng mát", "material": "Linen", "price": price,
            "variants": [{"size": "M", "color": "Trắng", "stockQuantity": stock}]}

def settings(tmp_path, key=""):
    return Settings(token="test-service-token-1234567890", gemini_key=key, embedding_mode="lexical", data_dir=tmp_path, knowledge_dir=KNOWLEDGE)

@pytest.mark.parametrize("text,value", [("dưới 500k",500000),("ngân sách 1,5 triệu",1500000),("không quá 500.000đ",500000),("tôi đi làm",None),("dưới 1.2.3 triệu",None)])
def test_budget(text,value):
    assert budget(text)==value

def test_no_key_uses_real_chroma_and_removes_unavailable_products(tmp_path):
    def blocked(request):
        pytest.fail("An offline test must not call any external API")
    engine=Engine(settings(tmp_path),httpx.Client(transport=httpx.MockTransport(blocked)))
    result=engine.advise(AdviseRequest(message="sơ mi đi làm dưới 500k",catalog=[product(),product(2,0),product(3,2,900000)]))
    assert result.productIds==[1]
    assert result.mode=="Gợi ý local"
    assert engine.advise(AdviseRequest(message="sơ mi",catalog=[product(1,0)])).productIds==[]

def test_gemini_ids_are_verified_and_personal_contact_is_redacted(tmp_path):
    captured=[]
    def handler(request):
        captured.append(json.loads(request.content))
        assert request.headers["x-goog-api-key"]=="test-key"
        return httpx.Response(200,json={"candidates":[{"content":{"parts":[{"text":json.dumps({"occasion":"work","productIds":[1,99999,1]})}]}}]})
    engine=Engine(settings(tmp_path,"test-key"),httpx.Client(transport=httpx.MockTransport(handler)))
    result=engine.advise(AdviseRequest(message="Tư vấn sơ mi, email test@example.com, số 0982655491",catalog=[product()]))
    assert result.productIds==[1]
    assert result.mode=="Gemini + RAG"
    text=captured[0]["contents"][0]["parts"][0]["text"]
    assert "test@example.com" not in text and "0982655491" not in text

@pytest.mark.parametrize("status,payload",[(429,{"error":"quota"}),(200,{"candidates":[{"content":{"parts":[{"text":"not json"}]}}]})])
def test_provider_failures_degrade_without_leaking_error_bodies(tmp_path,status,payload):
    engine=Engine(settings(tmp_path,"test-key"),httpx.Client(transport=httpx.MockTransport(lambda r:httpx.Response(status,json=payload))))
    result=engine.advise(AdviseRequest(message="sơ mi",catalog=[product()]))
    assert result.mode=="Gợi ý local" and result.productIds==[1]

def test_api_requires_service_token_and_health_does_not_expose_secrets(tmp_path):
    cfg=settings(tmp_path)
    engine=Engine(cfg,httpx.Client(transport=httpx.MockTransport(lambda r:pytest.fail("No external calls"))))
    with TestClient(create_app(cfg,engine)) as client:
        assert client.get("/health").status_code==401
        assert client.post("/fashion/advise",json={"message":"hi","catalog":[]}).status_code==401
        headers={"Authorization":"Bearer "+cfg.token}
        response=client.get("/health",headers=headers)
        assert response.json()["geminiConfigured"] is False
        assert cfg.token not in response.text
        response=client.post("/fashion/advise",headers=headers,json={"message":"áo sơ mi","catalog":[product()],"hipCm":92})
        assert response.status_code==200 and response.json()["productIds"]==[1]

def test_real_size_data_uses_the_four_provided_tables():
    charts=json.loads((KNOWLEDGE/"size-charts.json").read_text(encoding="utf-8"))
    assert [c["code"] for c in charts]==["SIZE_AO_NU","SIZE_QUAN_NU","SIZE_CHAN_VAY","SIZE_DAM"]
    assert charts[0]["rows"][-1]["ranges"]["bustCm"]=={"min":96,"max":102}
    assert charts[3]["rows"][-1]["ranges"]["hipCm"]=={"min":102,"max":108}

def test_remote_embeddings_are_cached_and_never_mix_with_lexical_vectors(tmp_path):
    from dataclasses import replace
    calls=[]
    def handler(request):
        calls.append(request.url.path)
        if request.url.path.endswith(":batchEmbedContents"):
            count=len(json.loads(request.content)["requests"])
            return httpx.Response(200,json={"embeddings":[{"values":[1.0]+[0.0]*767} for _ in range(count)]})
        return httpx.Response(200,json={"candidates":[{"content":{"parts":[{"text":'{"occasion":"work","productIds":[1]}'}]}}]})
    cfg=replace(settings(tmp_path,"test-key"),embedding_mode="gemini")
    engine=Engine(cfg,httpx.Client(transport=httpx.MockTransport(handler)))
    req=AdviseRequest(message="sơ mi công sở",catalog=[product()])
    assert engine.advise(req).productIds==[1]
    assert engine.advise(req).productIds==[1]
    assert sum(p.endswith(":batchEmbedContents") for p in calls)==2
