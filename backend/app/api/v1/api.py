from fastapi import APIRouter
from app.api.v1.endpoints import lyrics, separate, songs

api_router = APIRouter()
api_router.include_router(separate.router, prefix="/separate", tags=["Stem Separation"])
api_router.include_router(lyrics.router, prefix="/lyrics", tags=["Lyrics Search & Sync"])
api_router.include_router(songs.router, prefix="/songs", tags=["Saved Songs Library"])
