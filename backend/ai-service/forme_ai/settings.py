from dataclasses import dataclass
import os
from pathlib import Path


def load_env(path: Path) -> None:
    """Read literal dotenv values; never execute/source a file or log secrets."""
    if not path.is_file():
        return
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        key, sep, value = line.partition("=")
        if sep and key.strip() and key.strip() not in os.environ:
            os.environ[key.strip()] = value.strip().strip('"').strip("'")


@dataclass(frozen=True)
class Settings:
    token: str
    gemini_key: str = ""
    model: str = "gemini-3.5-flash-lite"
    embedding_model: str = "gemini-embedding-001"
    embedding_mode: str = "gemini"
    data_dir: Path = Path(".data")
    knowledge_dir: Path = Path(__file__).resolve().parents[2] / "src/main/resources/fashion"
    google_url: str = "https://generativelanguage.googleapis.com/v1beta"

    @classmethod
    def from_env(cls):
        backend = Path(__file__).resolve().parents[2]
        load_env(Path(os.environ.get("FORME_ENV_FILE", str(backend / ".env"))))
        return cls(
            token=os.getenv("AI_API_KEY", ""),
            gemini_key=os.getenv("GEMINI_API_KEY", ""),
            model=os.getenv("GEMINI_MODEL", "gemini-3.5-flash-lite"),
            embedding_model=os.getenv("GEMINI_EMBEDDING_MODEL", "gemini-embedding-001"),
            embedding_mode=os.getenv("AI_EMBEDDING_MODE", "gemini"),
            data_dir=Path(os.getenv("AI_DATA_DIRECTORY", str(backend / "ai-service/.data"))),
            knowledge_dir=Path(os.getenv("AI_KNOWLEDGE_DIRECTORY", str(backend / "src/main/resources/fashion"))),
        )
