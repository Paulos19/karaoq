import asyncio
import logging
import os
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path
from typing import Dict, Optional

from app.core.config import settings

logger = logging.getLogger("karaoq.demucs")
logging.basicConfig(level=logging.INFO)


class TaskStatusEnum(str, Enum):
    QUEUED = "QUEUED"
    PROCESSING = "PROCESSING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


@dataclass
class SeparationTask:
    task_id: str
    original_filename: str
    input_path: Path
    status: TaskStatusEnum = TaskStatusEnum.QUEUED
    progress_percentage: int = 0
    message: str = "Aguardando na fila de processamento..."
    vocals_relative_path: Optional[str] = None
    instrumental_relative_path: Optional[str] = None
    created_at: float = field(default_factory=time.time)
    updated_at: float = field(default_factory=time.time)
    error: Optional[str] = None


class DemucsService:
    def __init__(self):
        self._tasks: Dict[str, SeparationTask] = {}
        self._lock = asyncio.Lock()

    def get_task(self, task_id: str) -> Optional[SeparationTask]:
        return self._tasks.get(task_id)

    async def register_task(self, task_id: str, original_filename: str, input_path: Path) -> SeparationTask:
        async with self._lock:
            task = SeparationTask(
                task_id=task_id,
                original_filename=original_filename,
                input_path=input_path,
                status=TaskStatusEnum.QUEUED,
                message="Arquivo recebido. Na fila para separação."
            )
            self._tasks[task_id] = task
            return task

    async def update_task(self, task_id: str, **kwargs):
        async with self._lock:
            if task_id in self._tasks:
                task = self._tasks[task_id]
                for key, value in kwargs.items():
                    if hasattr(task, key):
                        setattr(task, key, value)
                task.updated_at = time.time()

    async def execute_separation(self, task_id: str):
        """
        Executa a separação de áudio assincronamente usando o Demucs.
        Prioriza htdemucs com --two-stems=vocals para isolar 'vocals' e 'no_vocals' (instrumental).
        """
        task = self.get_task(task_id)
        if not task:
            logger.error(f"Tarefa {task_id} não encontrada para separação.")
            return

        await self.update_task(
            task_id,
            status=TaskStatusEnum.PROCESSING,
            progress_percentage=15,
            message="Iniciando modelo de IA (htdemucs 2-stems)..."
        )

        task_output_dir = settings.SEPARATED_DIR / task_id
        task_output_dir.mkdir(parents=True, exist_ok=True)

        try:
            # Comando demucs
            # demucs -n <model> --two-stems=vocals --mp3 --mp3-bitrate 320 -o <task_output_dir> <input_file>
            device_arg = []
            if settings.DEMUCS_DEVICE != "auto":
                device_arg = ["-d", settings.DEMUCS_DEVICE]

            cmd = [
                sys.executable, "-m", "demucs.separate",
                "-n", settings.DEMUCS_MODEL,
                f"--two-stems={settings.DEMUCS_TWO_STEMS}",
                "--mp3",
                f"--mp3-bitrate={settings.DEMUCS_MP3_BITRATE}",
                "-o", str(task_output_dir),
                *device_arg,
                str(task.input_path)
            ]

            logger.info(f"Executando comando Demucs para tarefa {task_id}: {' '.join(cmd)}")

            await self.update_task(
                task_id,
                progress_percentage=35,
                message="Separando faixas de voz e instrumental com IA..."
            )

            # Executa o subprocesso de forma assíncrona
            process = await asyncio.create_subprocess_exec(
                *cmd,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE
            )

            stdout, stderr = await process.communicate()

            if process.returncode != 0:
                err_msg = stderr.decode('utf-8', errors='replace')
                logger.error(f"Erro na execução do Demucs para tarefa {task_id}: {err_msg}")
                # Verifica se o demucs está instalado; se não estiver, exibe instrução clara
                if "No module named demucs" in err_msg:
                    err_msg = "Módulo Demucs não instalado no ambiente Python. Instale com 'pip install -r requirements.txt'."
                await self.update_task(
                    task_id,
                    status=TaskStatusEnum.FAILED,
                    message="Falha no processamento de IA.",
                    error=err_msg
                )
                return

            await self.update_task(
                task_id,
                progress_percentage=85,
                message="Organizando arquivos de saída gerados..."
            )

            # Demucs cria uma pasta: <task_output_dir>/<model>/<input_stem>/[vocals.mp3, no_vocals.mp3]
            model_dir = task_output_dir / settings.DEMUCS_MODEL
            input_stem_dir = None

            if model_dir.exists():
                subdirs = [d for d in model_dir.iterdir() if d.is_dir()]
                if subdirs:
                    input_stem_dir = subdirs[0]

            if not input_stem_dir or not input_stem_dir.exists():
                raise FileNotFoundError(f"Diretório de saída do Demucs não encontrado em {model_dir}")

            vocals_source = input_stem_dir / "vocals.mp3"
            no_vocals_source = input_stem_dir / "no_vocals.mp3"

            # Destino padronizado dentro de /storage/separated/{task_id}/
            dest_vocals = task_output_dir / "vocals.mp3"
            dest_instrumental = task_output_dir / "instrumental.mp3"

            if vocals_source.exists():
                shutil.copy2(vocals_source, dest_vocals)
            if no_vocals_source.exists():
                shutil.copy2(no_vocals_source, dest_instrumental)

            # Caminhos relativos para URLs da API
            vocals_rel = f"separated/{task_id}/vocals.mp3"
            inst_rel = f"separated/{task_id}/instrumental.mp3"

            await self.update_task(
                task_id,
                status=TaskStatusEnum.COMPLETED,
                progress_percentage=100,
                message="Faixas separadas com sucesso!",
                vocals_relative_path=vocals_rel,
                instrumental_relative_path=inst_rel
            )
            logger.info(f"Tarefa {task_id} finalizada com sucesso. Stems: {vocals_rel}, {inst_rel}")

        except Exception as ex:
            logger.exception(f"Exceção inesperada na separação da tarefa {task_id}: {str(ex)}")
            await self.update_task(
                task_id,
                status=TaskStatusEnum.FAILED,
                message="Erro interno durante a separação.",
                error=str(ex)
            )


demucs_service = DemucsService()
