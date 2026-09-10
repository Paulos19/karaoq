from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.api.v1.api import api_router
from app.core.config import settings


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Inicialização: assegura que as pastas existem
    settings.setup_directories()
    yield
    # Finalização se necessário


app = FastAPI(
    title=settings.PROJECT_NAME,
    description="KaraoQ AI Audio Separation Backend (Demucs htdemucs 2-stems engine)",
    version="1.0.0",
    lifespan=lifespan
)

# Configuração de CORS para permitir acesso direto do app Android e navegadores
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS if isinstance(settings.CORS_ORIGINS, list) else [settings.CORS_ORIGINS],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Monta o diretório de armazenamento como estático para streaming de áudio
app.mount("/storage", StaticFiles(directory=str(settings.STORAGE_DIR)), name="storage")

# Registra os roteadores da API v1
app.include_router(api_router, prefix=settings.API_V1_STR)


@app.get("/", tags=["Health"])
async def root():
    return {
        "app": settings.PROJECT_NAME,
        "version": "1.0.0",
        "status": "online",
        "endpoints": {
            "docs": "/docs",
            "separate": f"{settings.API_V1_STR}/separate"
        }
    }


@app.get("/health", tags=["Health"])
async def health_check():
    return {
        "status": "healthy",
        "demucs_model": settings.DEMUCS_MODEL,
        "two_stems_mode": settings.DEMUCS_TWO_STEMS
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host=settings.HOST, port=settings.PORT, reload=settings.DEBUG)
