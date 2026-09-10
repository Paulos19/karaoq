from typing import Any, Dict, List, Optional
from fastapi import APIRouter, HTTPException, Query, status
from pydantic import BaseModel

from app.services.lyrics_service import lyrics_service

router = APIRouter()


class LyricLineModel(BaseModel):
    time_ms: int
    text: str


class LyricsSearchResponse(BaseModel):
    source: str
    is_synced: bool
    artist: str
    title: str
    lines: List[LyricLineModel]
    raw_lrc: Optional[str] = None
    plain_text: str


@router.get("/search", response_model=LyricsSearchResponse)
async def search_lyrics(
    artist: str = Query(..., description="Nome do cantor ou banda"),
    title: str = Query(..., description="Título da música"),
    duration: Optional[int] = Query(None, description="Duração estimada da música em segundos")
):
    """
    Pesquisa a letra da música por Artista e Título.
    Prioriza letras com sincronização de tempo (LRC / timestamps).
    """
    if not artist.strip() or not title.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Artista e Título da música são obrigatórios para a busca."
        )

    result = await lyrics_service.search_lyrics(
        artist=artist,
        title=title,
        duration_seconds=duration
    )

    return LyricsSearchResponse(
        source=result.get("source", "none"),
        is_synced=result.get("is_synced", False),
        artist=result.get("artist", artist),
        title=result.get("title", title),
        lines=[LyricLineModel(**line) for line in result.get("lines", [])],
        raw_lrc=result.get("raw_lrc"),
        plain_text=result.get("plain_text", "")
    )
