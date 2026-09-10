import os
import uuid
import aiofiles
from pathlib import Path
from typing import Optional
from fastapi import APIRouter, BackgroundTasks, File, HTTPException, Request, UploadFile, status
from fastapi.responses import FileResponse
from pydantic import BaseModel

from app.core.config import settings
from app.services.demucs_service import DemucsService, TaskStatusEnum, demucs_service

router = APIRouter()

ALLOWED_EXTENSIONS = {".mp3", ".wav", ".m4a", ".ogg", ".flac", ".aac"}
MAX_FILE_SIZE_MB = 100  # 100 MB max for audio files


class SeparationCreateResponse(BaseModel):
    task_id: str
    filename: str
    status: TaskStatusEnum
    message: str


class SeparationStatusResponse(BaseModel):
    task_id: str
    original_filename: str
    status: TaskStatusEnum
    progress_percentage: int
    message: str
    vocals_url: Optional[str] = None
    instrumental_url: Optional[str] = None
    error: Optional[str] = None


@router.post("", response_model=SeparationCreateResponse, status_code=status.HTTP_202_ACCEPTED)
async def create_separation_task(
    background_tasks: BackgroundTasks,
    audio_file: UploadFile = File(...)
):
    """
    Recebe um arquivo de áudio e enfileira a separação de stems com IA (Demucs).
    Retorna imediatamente o `task_id` para acompanhamento.
    """
    if not audio_file.filename:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Arquivo de áudio inválido (sem nome)."
        )

    file_ext = Path(audio_file.filename).suffix.lower()
    if file_ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Formato não suportado '{file_ext}'. Formatos válidos: {', '.join(ALLOWED_EXTENSIONS)}"
        )

    task_id = str(uuid.uuid4())
    safe_filename = f"{task_id}_{Path(audio_file.filename).name}"
    upload_file_path = settings.UPLOAD_DIR / safe_filename

    # Salva o arquivo de upload de forma assíncrona
    try:
        async with aiofiles.open(upload_file_path, "wb") as out_file:
            while content := await audio_file.read(1024 * 1024):  # 1MB chunks
                await out_file.write(content)
    except Exception as ex:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Falha ao salvar o arquivo enviado: {str(ex)}"
        )

    # Registra a tarefa no serviço Demucs
    task = await demucs_service.register_task(
        task_id=task_id,
        original_filename=audio_file.filename,
        input_path=upload_file_path
    )

    # Enfileira a tarefa assíncrona de separação
    background_tasks.add_task(demucs_service.execute_separation, task_id)

    return SeparationCreateResponse(
        task_id=task.task_id,
        filename=task.original_filename,
        status=task.status,
        message="Arquivo recebido. Processamento em background iniciado."
    )


@router.get("/{task_id}", response_model=SeparationStatusResponse)
async def get_separation_status(task_id: str, request: Request):
    """
    Consulta o status atual e o progresso da separação de stems.
    Quando concluído, retorna as URLs para streaming e download do vocal e instrumental.
    """
    task = demucs_service.get_task(task_id)
    if not task:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Tarefa com ID '{task_id}' não encontrada."
        )

    forwarded_proto = request.headers.get("x-forwarded-proto")
    forwarded_host = request.headers.get("x-forwarded-host")

    scheme = forwarded_proto or request.url.scheme
    host = forwarded_host or request.headers.get("host") or request.url.netloc
    base_url = f"{scheme}://{host}".rstrip("/")

    vocals_url = None
    instrumental_url = None

    if task.status == TaskStatusEnum.COMPLETED:
        if task.vocals_relative_path:
            vocals_url = f"{base_url}/storage/{task.vocals_relative_path}"
        if task.instrumental_relative_path:
            instrumental_url = f"{base_url}/storage/{task.instrumental_relative_path}"

    return SeparationStatusResponse(
        task_id=task.task_id,
        original_filename=task.original_filename,
        status=task.status,
        progress_percentage=task.progress_percentage,
        message=task.message,
        vocals_url=vocals_url,
        instrumental_url=instrumental_url,
        error=task.error
    )


@router.get("/{task_id}/download/{stem_type}")
async def download_stem(task_id: str, stem_type: str):
    """
    Download direto ou streaming do arquivo de áudio gerado (`vocals` ou `instrumental`).
    """
    task = demucs_service.get_task(task_id)
    if not task:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Tarefa '{task_id}' não encontrada."
        )

    if task.status != TaskStatusEnum.COMPLETED:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"A tarefa ainda não foi concluída. Status atual: {task.status}"
        )

    stem_type = stem_type.lower()
    if stem_type in ["vocals", "vocal"]:
        file_path = settings.SEPARATED_DIR / task_id / "vocals.mp3"
        filename = f"{Path(task.original_filename).stem}_vocals.mp3"
    elif stem_type in ["instrumental", "no_vocals", "playback"]:
        file_path = settings.SEPARATED_DIR / task_id / "instrumental.mp3"
        filename = f"{Path(task.original_filename).stem}_instrumental.mp3"
    else:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Tipo de stem inválido. Use 'vocals' ou 'instrumental'."
        )

    if not file_path.exists():
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Arquivo de áudio não encontrado no disco."
        )

    return FileResponse(
        path=str(file_path),
        media_type="audio/mpeg",
        filename=filename
    )
