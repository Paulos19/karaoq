from typing import Any, Dict, List, Optional
import logging
from fastapi import APIRouter, HTTPException, Query, WebSocket, WebSocketDisconnect, status
from pydantic import BaseModel

from app.services.ai_transcription_service import ai_transcription_service
from app.services.lyrics_service import lyrics_service
from app.services.whisper_service import whisper_service

logger = logging.getLogger("karaoq.lyrics")
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


@router.post("/transcribe/{task_id}", response_model=LyricsSearchResponse)
async def transcribe_lyrics_with_ai(
    task_id: str,
    artist: Optional[str] = Query("", description="Nome do cantor"),
    title: Optional[str] = Query("", description="Título da faixa")
):
    """
    Transcreve a voz isolada gerada pelo Demucs em letra sincronizada LRC
    utilizando Whisper com fallback para Gemini Audio.
    """
    try:
        try:
            result = await whisper_service.transcribe_vocals_to_lrc(task_id)
        except Exception as whisper_err:
            logger.warning(f"Whisper falhou ou não disponível ({whisper_err}), tentando Gemini Audio...")
            if ai_transcription_service.is_configured():
                result = await ai_transcription_service.transcribe_vocals_to_lrc(task_id)
            else:
                raise whisper_err

        return LyricsSearchResponse(
            source=result.get("source", "whisper"),
            is_synced=result.get("is_synced", True),
            artist=artist or "IA Transcrita",
            title=title or "Faixa Transcrita",
            lines=[LyricLineModel(**line) for line in result.get("lines", [])],
            raw_lrc=result.get("raw_lrc"),
            plain_text=result.get("plain_text", "")
        )
    except FileNotFoundError as fnf:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(fnf))
    except ValueError as ve:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(ve))
    except Exception as ex:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Falha ao transcrever com IA: {str(ex)}"
        )


@router.websocket("/ws/transcribe/{task_id}")
async def websocket_transcribe(websocket: WebSocket, task_id: str):
    """
    Canal WebSocket para streaming em tempo real do progresso e dos versos detectados
    durante a transcrição da voz isolada com Whisper.
    """
    await websocket.accept()
    logger.info(f"Cliente WebSocket conectado para transcrição Whisper da tarefa {task_id}")
    try:
        async def on_progress(data: Dict[str, Any]):
            try:
                await websocket.send_json(data)
            except Exception as send_err:
                logger.warning(f"Erro ao enviar atualização no WebSocket de transcrição: {send_err}")

        # Inicia a transcrição assíncrona com callbacks em tempo real
        result = await whisper_service.transcribe_vocals_to_lrc(task_id, on_progress=on_progress)

        # Envia o pacote consolidado final e encerra a conexão normalmente
        await websocket.send_json({
            "status": "completed",
            "progress": 100.0,
            "task_id": task_id,
            "result": {
                "source": result.get("source", "whisper"),
                "is_synced": True,
                "lines": result.get("lines", []),
                "raw_lrc": result.get("raw_lrc"),
                "plain_text": result.get("plain_text", "")
            }
        })
        await websocket.close()
    except WebSocketDisconnect:
        logger.info(f"Cliente WebSocket desconectado da transcrição {task_id}")
    except Exception as ex:
        logger.error(f"Erro no WebSocket de transcrição para {task_id}: {ex}")
        try:
            await websocket.send_json({
                "status": "error",
                "progress": 0.0,
                "message": str(ex),
                "task_id": task_id
            })
            await websocket.close()
        except Exception:
            pass
