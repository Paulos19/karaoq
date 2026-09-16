from typing import Any, Dict, List, Optional
from fastapi import APIRouter, HTTPException, Query, status
from pydantic import BaseModel, Field

from app.services.leaderboard_service import leaderboard_service

router = APIRouter()


class LeaderboardSubmitPayload(BaseModel):
    singer_name: str = Field(..., max_length=50, description="Nome ou apelido do cantor")
    score: int = Field(..., ge=0, le=10000, description="Pontuação obtida (0 a 10.000)")
    rank: str = Field("C", description="Classificação obtida (S, A, B, C)")
    max_combo: int = Field(0, ge=0, description="Maior sequência de combo atingida")
    perfect_hits: int = Field(0, ge=0, description="Quantidade de acertos perfeitos")


class LeaderboardEntryResponse(BaseModel):
    id: str
    song_id: str
    singer_name: str
    score: int
    rank: str
    max_combo: int
    perfect_hits: int
    created_at: float


@router.get("/{song_id}/leaderboard", response_model=List[LeaderboardEntryResponse])
async def get_song_leaderboard(
    song_id: str,
    limit: int = Query(10, ge=1, le=50, description="Quantidade máxima de recordes no Top")
):
    """
    Retorna o ranking global dos melhores cantores para a música especificada.
    """
    entries = leaderboard_service.get_top_scores(song_id, limit=limit)
    return [LeaderboardEntryResponse(**e) for e in entries]


@router.post("/{song_id}/leaderboard", response_model=LeaderboardEntryResponse, status_code=status.HTTP_201_CREATED)
async def submit_song_score(
    song_id: str,
    payload: LeaderboardSubmitPayload
):
    """
    Registra a pontuação e performance de um cantor no placar da música.
    """
    entry = leaderboard_service.submit_score(
        song_id=song_id,
        singer_name=payload.singer_name,
        score=payload.score,
        rank=payload.rank,
        max_combo=payload.max_combo,
        perfect_hits=payload.perfect_hits
    )
    return LeaderboardEntryResponse(**entry)
