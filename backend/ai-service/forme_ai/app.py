from contextlib import asynccontextmanager
import secrets
import httpx
from fastapi import FastAPI, Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from .settings import Settings
from .models import AdviseRequest, Selection
from .engine import Engine


def create_app(settings: Settings | None = None, engine=None):
    settings = settings or Settings.from_env()
    if len(settings.token) < 24:
        raise RuntimeError("Set AI_API_KEY to a service token of at least 24 characters. Do not use the Gemini key here.")
    bearer = HTTPBearer(auto_error=False)

    def authorize(value: HTTPAuthorizationCredentials | None = Depends(bearer)):
        if value is None or not secrets.compare_digest(value.credentials, settings.token):
            raise HTTPException(status_code=401, detail="Dịch vụ AI yêu cầu xác thực.")

    @asynccontextmanager
    async def lifespan(app):
        client = httpx.Client(trust_env=False, follow_redirects=False)
        app.state.engine = engine or Engine(settings, client)
        yield
        client.close()

    app = FastAPI(title="FORME Fashion AI", version="1.0.0", lifespan=lifespan, dependencies=[Depends(authorize)])

    @app.get("/health")
    def health():
        return {"status": "UP", "geminiConfigured": bool(settings.gemini_key), "model": settings.model, "embeddingMode": settings.embedding_mode if settings.gemini_key else "lexical"}

    @app.post("/fashion/advise", response_model=Selection)
    def advise(request: AdviseRequest):
        return app.state.engine.advise(request)

    return app
