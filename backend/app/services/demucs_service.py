import asyncio
import logging
import os
import re
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path
from typing import Dict, List, Optional

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


def convert_wav_to_mp3(wav_path: Path, mp3_path: Path, bitrate_kbps: int = 320) -> bool:
    """
    Converte um arquivo WAV para MP3 usando lameenc nativo de alta performance.
    """
    try:
        import lameenc
        import wave
        with wave.open(str(wav_path), "rb") as w:
            nchannels = w.getnchannels()
            sampwidth = w.getsampwidth()
            framerate = w.getframerate()
            frames = w.readframes(w.getnframes())

        if sampwidth == 2:  # 16-bit PCM
            encoder = lameenc.Encoder()
            encoder.set_bit_rate(bitrate_kbps)
            encoder.set_in_sample_rate(framerate)
            encoder.set_channels(nchannels)
            encoder.set_quality(2)
            mp3_bytes = encoder.encode(frames)
            mp3_bytes += encoder.flush()
            with open(mp3_path, "wb") as f:
                f.write(mp3_bytes)
            return True
    except Exception as ex:
        logger.warning(f"Falha ao converter {wav_path} para MP3 via lameenc: {ex}")
    return False


class DemucsService:
    def __init__(self):
        self._tasks: Dict[str, SeparationTask] = {}
        self._lock = asyncio.Lock()
        self._subscribers: Dict[str, List[asyncio.Queue]] = {}

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

    async def subscribe(self, task_id: str) -> asyncio.Queue:
        queue: asyncio.Queue = asyncio.Queue()
        async with self._lock:
            if task_id not in self._subscribers:
                self._subscribers[task_id] = []
            self._subscribers[task_id].append(queue)
        return queue

    async def unsubscribe(self, task_id: str, queue: asyncio.Queue):
        async with self._lock:
            if task_id in self._subscribers and queue in self._subscribers[task_id]:
                self._subscribers[task_id].remove(queue)
                if not self._subscribers[task_id]:
                    del self._subscribers[task_id]

    async def update_task(self, task_id: str, **kwargs):
        async with self._lock:
            if task_id in self._tasks:
                task = self._tasks[task_id]
                for key, value in kwargs.items():
                    if hasattr(task, key):
                        setattr(task, key, value)
                task.updated_at = time.time()

                # Notifica todos os WebSockets inscritos nesta tarefa
                if task_id in self._subscribers:
                    for queue in list(self._subscribers[task_id]):
                        await queue.put(task)

    async def execute_separation(self, task_id: str):
        """
        Executa a separação de áudio assincronamente usando o Demucs com:
        1. Streaming de progresso real via leitura de stderr (evitando timeout de proxies reversos).
        2. Limites de memória e segmentação para prevenir crash por OOM.
        3. Validação rigorosa dos arquivos gerados (impedindo 'sem áudio' / 404).
        """
        task = self.get_task(task_id)
        if not task:
            logger.error(f"Tarefa {task_id} não encontrada para separação.")
            return

        await self.update_task(
            task_id,
            status=TaskStatusEnum.PROCESSING,
            progress_percentage=15,
            message="Carregando modelo de IA (htdemucs)..."
        )

        task_output_dir = settings.SEPARATED_DIR / task_id
        task_output_dir.mkdir(parents=True, exist_ok=True)

        try:
            device_arg = []
            if settings.DEMUCS_DEVICE != "auto":
                device_arg = ["-d", settings.DEMUCS_DEVICE]

            # Parâmetros otimizados:
            # - -j 1: previne saturação de CPU/threads em VPS e reduz consumo de RAM
            # - --filename: garante padrão consistente de saída dos stems
            # Nota: HTDemucs utiliza nativamente segmentos de 7.8s (o máximo permitido pela arquitetura Transformer)
            cmd = [
                sys.executable, "-m", "demucs.separate",
                "-n", settings.DEMUCS_MODEL,
                f"--two-stems={settings.DEMUCS_TWO_STEMS}",
                "--filename", "{stem}.{ext}",
                "-j", "1",
                "--mp3",
                f"--mp3-bitrate={settings.DEMUCS_MP3_BITRATE}",
                "-o", str(task_output_dir),
                *device_arg,
                str(task.input_path)
            ]

            logger.info(f"Iniciando comando Demucs para tarefa {task_id}: {' '.join(cmd)}")

            await self.update_task(
                task_id,
                progress_percentage=30,
                message="Iniciando separação com IA..."
            )

            # Inicia o subprocesso
            process = await asyncio.create_subprocess_exec(
                *cmd,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE
            )

            stderr_chunks = []
            progress_regex = re.compile(r"(\d{1,3})%")
            last_progress_time = time.time()
            last_pct_sent = 30

            # Monitora o stderr em tempo real para capturar as atualizações da barra de progresso do Demucs
            async def monitor_stderr():
                nonlocal last_progress_time, last_pct_sent
                while True:
                    chunk = await process.stderr.read(256)
                    if not chunk:
                        break
                    text = chunk.decode("utf-8", errors="replace")
                    stderr_chunks.append(text)

                    # Verifica porcentagem
                    matches = progress_regex.findall(text)
                    if matches:
                        try:
                            demucs_pct = int(matches[-1])
                            now = time.time()
                            # Envia atualizações a cada 1.5s ou se houver salto relevante
                            if now - last_progress_time >= 1.5 or demucs_pct >= 99:
                                overall_pct = 30 + int(demucs_pct * 0.55)  # 30% a 85%
                                if overall_pct != last_pct_sent:
                                    last_pct_sent = overall_pct
                                    last_progress_time = now
                                    await self.update_task(
                                        task_id,
                                        progress_percentage=overall_pct,
                                        message=f"Separando faixas com IA ({demucs_pct}%)..."
                                    )
                        except Exception:
                            pass

            # Aguarda a leitura contínua de stderr e a conclusão do processo
            await asyncio.gather(monitor_stderr(), process.wait())
            stderr_output = "".join(stderr_chunks)

            if process.returncode != 0:
                logger.error(f"Erro na execução do Demucs para tarefa {task_id}: {stderr_output}")
                err_msg = stderr_output
                if "No module named demucs" in stderr_output:
                    err_msg = "Módulo Demucs não instalado no ambiente Python."
                elif not err_msg.strip():
                    err_msg = "Processo de IA interrompido inesperadamente (possível falta de memória)."
                await self.update_task(
                    task_id,
                    status=TaskStatusEnum.FAILED,
                    message="Falha no processamento de IA.",
                    error=err_msg
                )
                return

            await self.update_task(
                task_id,
                progress_percentage=88,
                message="Processando e validando stems gerados..."
            )

            # Localiza os arquivos gerados (podem estar em task_output_dir ou subpastas de modelo)
            vocals_source: Optional[Path] = None
            no_vocals_source: Optional[Path] = None

            search_dirs = [task_output_dir]
            model_dir = task_output_dir / settings.DEMUCS_MODEL
            if model_dir.exists():
                search_dirs.append(model_dir)
                search_dirs.extend([d for d in model_dir.iterdir() if d.is_dir()])

            for d in search_dirs:
                if not d.exists():
                    continue
                for f in d.iterdir():
                    if f.is_file():
                        fname = f.name.lower()
                        if "vocals" in fname and "no_vocals" not in fname:
                            vocals_source = f
                        elif "no_vocals" in fname or "instrumental" in fname:
                            no_vocals_source = f

            if not vocals_source or not no_vocals_source:
                raise FileNotFoundError(
                    f"Arquivos gerados pelo Demucs não foram encontrados no diretório de saída: {task_output_dir}"
                )

            dest_vocals = task_output_dir / "vocals.mp3"
            dest_instrumental = task_output_dir / "instrumental.mp3"

            # Copia ou converte vocal
            if vocals_source.suffix.lower() == ".mp3":
                shutil.copy2(vocals_source, dest_vocals)
            else:
                if not convert_wav_to_mp3(vocals_source, dest_vocals):
                    shutil.copy2(vocals_source, dest_vocals)

            # Copia ou converte instrumental
            if no_vocals_source.suffix.lower() == ".mp3":
                shutil.copy2(no_vocals_source, dest_instrumental)
            else:
                if not convert_wav_to_mp3(no_vocals_source, dest_instrumental):
                    shutil.copy2(no_vocals_source, dest_instrumental)

            # Validação rigorosa: assegura que os arquivos existem e têm áudio real (> 1000 bytes)
            if not dest_vocals.exists() or dest_vocals.stat().st_size < 1000:
                raise RuntimeError("Arquivo de voz gerado está vazio ou ausente.")
            if not dest_instrumental.exists() or dest_instrumental.stat().st_size < 1000:
                raise RuntimeError("Arquivo instrumental gerado está vazio ou ausente.")

            # Caminhos relativos padrão da API
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
            logger.info(
                f"Tarefa {task_id} finalizada com sucesso. Stems validados: "
                f"vocals={dest_vocals.stat().st_size}B, instrumental={dest_instrumental.stat().st_size}B"
            )

        except Exception as ex:
            logger.exception(f"Exceção inesperada na separação da tarefa {task_id}: {str(ex)}")
            await self.update_task(
                task_id,
                status=TaskStatusEnum.FAILED,
                message="Erro interno durante a separação.",
                error=str(ex)
            )


demucs_service = DemucsService()

