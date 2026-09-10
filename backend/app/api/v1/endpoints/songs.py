from typing import Any, Dict, List, Optional
from fastapi import APIRouter, HTTPException, Request, status
from pydantic import BaseModel, Field

from app.services.song_storage_service import song_storage_service

router = APIRouter()


class LyricLinePayload(BaseModel):
    time_ms: int
    text: str


class SongLyricsPayload(BaseModel):
    is_synced: bool
    lines: List[LyricLinePayload] = Field(default_factory=list)
    plain_text: str = ""
    raw_lrc: Optional[str] = None


class SongCreatePayload(BaseModel):
    id: str
    title: str
    artist: str
    duration_ms: Optional[int] = 0
    vocals_url: Optional[str] = None
    instrumental_url: Optional[str] = None
    lyrics: Optional[SongLyricsPayload] = None


class SongResponse(BaseModel):
    id: str
    title: str
    artist: str
    duration_ms: int = 0
    vocals_url: Optional[str] = None
    instrumental_url: Optional[str] = None
    lyrics: Optional[SongLyricsPayload] = None
    created_at: float
    updated_at: Optional[float] = None


def resolve_audio_urls(song_dict: Dict[str, Any], request: Request) -> Dict[str, Any]:
    """
    Garante que as URLs de vocals e instrumental estejam completas e resolvidas com o host correto.
    """
    data = dict(song_dict)
    forwarded_proto = request.headers.get("x-forwarded-proto")
    forwarded_host = request.headers.get("x-forwarded-host")
    scheme = forwarded_proto or request.url.scheme
    host = forwarded_host or request.headers.get("host") or request.url.netloc
    base_url = f"{scheme}://{host}".rstrip("/")

    song_id = data.get("id")
    if not data.get("vocals_url") and song_id:
        data["vocals_url"] = f"{base_url}/storage/separated/{song_id}/vocals.mp3"
    if not data.get("instrumental_url") and song_id:
        data["instrumental_url"] = f"{base_url}/storage/separated/{song_id}/instrumental.mp3"

    return data


@router.get("", response_model=List[SongResponse])
async def list_saved_songs(request: Request):
    """
    Lista todas as músicas salvas no storage da VPS/servidor.
    """
    songs = song_storage_service.list_songs()
    resolved = [resolve_audio_urls(s, request) for s in songs]
    return resolved


@router.get("/{song_id}", response_model=SongResponse)
async def get_saved_song(song_id: str, request: Request):
    """
    Recupera os detalhes e letras de uma música salva.
    """
    song = song_storage_service.get_song(song_id)
    if not song:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Música com ID '{song_id}' não encontrada no storage."
        )
    return resolve_audio_urls(song, request)


@router.post("", response_model=SongResponse, status_code=status.HTTP_201_CREATED)
async def save_song(payload: SongCreatePayload, request: Request):
    """
    Salva uma música pós-separação com seus metadados de artista, título e letra.
    """
    saved = song_storage_service.save_song(payload.model_dump())
    return resolve_audio_urls(saved, request)


@router.delete("/{song_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_saved_song(song_id: str):
    """
    Exclui uma música e seus arquivos do storage.
    """
    success = song_storage_service.delete_song(song_id)
    if not success:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Música com ID '{song_id}' não encontrada."
        )
    return None
