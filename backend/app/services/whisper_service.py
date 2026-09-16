import asyncio
import logging
import os
from pathlib import Path
from typing import Any, Callable, Coroutine, Dict, List, Optional

from app.core.config import settings
from app.services.lyrics_service import lyrics_service

logger = logging.getLogger("karaoq.whisper")


class WhisperService:
    """
    Serviço de transcrição e alinhamento de letras com Whisper (Faster-Whisper / OpenAI Whisper).
    Executa localmente sem depender de chaves externas de API, gerando timestamps precisos
    no formato LRC padrão a partir da faixa vocal isolada.
    """

    def __init__(self):
        self._model = None
        self._model_size = os.environ.get("WHISPER_MODEL_SIZE", "base")
        self._lock = asyncio.Lock()

    def _get_model(self):
        if self._model is not None:
            return self._model

        logger.info(f"Carregando modelo Whisper ({self._model_size})...")
        try:
            from faster_whisper import WhisperModel
            # Tenta carregar faster-whisper com quantização int8 para alta velocidade na CPU
            self._model = WhisperModel(self._model_size, device="cpu", compute_type="int8")
            logger.info(f"Modelo Faster-Whisper ({self._model_size}) carregado com sucesso.")
            return self._model
        except ImportError:
            logger.warning("faster-whisper não instalado. Tentando carregar openai-whisper...")

        try:
            import whisper
            self._model = whisper.load_model(self._model_size)
            logger.info(f"Modelo OpenAI Whisper ({self._model_size}) carregado com sucesso.")
            return self._model
        except ImportError:
            raise ImportError(
                "Nenhum pacote Whisper instalado. Instale 'faster-whisper' via pip install faster-whisper."
            )

    async def transcribe_vocals_to_lrc(
        self,
        task_id: str,
        on_progress: Optional[Callable[[Dict[str, Any]], Coroutine[Any, Any, None]]] = None
    ) -> Dict[str, Any]:
        """
        Transcreve o arquivo vocals.mp3 de uma tarefa em versos sincronizados LRC com timestamps precisos.
        Se on_progress for fornecido, emite atualizações parciais durante a transcrição.
        """
        vocals_path = settings.SEPARATED_DIR / task_id / "vocals.mp3"
        if not vocals_path.exists():
            raise FileNotFoundError(f"Arquivo de voz isolada não encontrado em {vocals_path}")

        loop = asyncio.get_running_loop()

        if on_progress:
            await on_progress({
                "status": "starting",
                "progress": 5.0,
                "message": "Inicializando modelo Whisper para transcrição...",
                "task_id": task_id
            })

        # Executa em thread pool para não bloquear o loop de eventos assíncrono
        def _transcribe_sync():
            model = self._get_model()
            lrc_lines: List[str] = []
            parsed_lines: List[Dict[str, Any]] = []

            # Verifica se é Faster-Whisper
            if hasattr(model, "transcribe"):
                try:
                    # Chamada do Faster-Whisper
                    segments, info = model.transcribe(
                        str(vocals_path),
                        beam_size=5,
                        vad_filter=True,
                        vad_parameters=dict(min_silence_duration_ms=400)
                    )

                    duration = info.duration if hasattr(info, "duration") and info.duration > 0 else 180.0
                    detected_lang = getattr(info, "language", "pt")

                    for seg in segments:
                        text = seg.text.strip()
                        if not text:
                            continue

                        start_sec = max(0.0, float(seg.start))
                        time_ms = int(start_sec * 1000)

                        mins = int(start_sec // 60)
                        secs = start_sec % 60
                        lrc_timestamp = f"[{mins:02d}:{secs:05.2f}]"
                        lrc_line = f"{lrc_timestamp} {text}"

                        lrc_lines.append(lrc_line)
                        parsed_lines.append({
                            "time_ms": time_ms,
                            "text": text
                        })

                        progress_pct = min(98.0, max(10.0, (start_sec / duration) * 100.0))

                        if on_progress:
                            # Dispara callback de progresso de forma thread-safe
                            asyncio.run_coroutine_threadsafe(
                                on_progress({
                                    "status": "transcribing",
                                    "progress": round(progress_pct, 1),
                                    "time_ms": time_ms,
                                    "text": text,
                                    "lrc_line": lrc_line,
                                    "language": detected_lang
                                }),
                                loop
                            )

                    raw_lrc = "\n".join(lrc_lines)
                    plain_text = "\n".join([p["text"] for p in parsed_lines])

                    return {
                        "source": "whisper",
                        "is_synced": True,
                        "language": detected_lang,
                        "lines": parsed_lines,
                        "raw_lrc": raw_lrc,
                        "plain_text": plain_text
                    }

                except TypeError:
                    # Fallback para OpenAI Whisper se a assinatura de transcribe for diferente
                    pass

            # Fallback para OpenAI Whisper padrão
            import whisper
            result = model.transcribe(str(vocals_path), verbose=False)
            segments = result.get("segments", [])
            detected_lang = result.get("language", "pt")

            for seg in segments:
                text = seg.get("text", "").strip()
                if not text:
                    continue
                start_sec = float(seg.get("start", 0.0))
                time_ms = int(start_sec * 1000)

                mins = int(start_sec // 60)
                secs = start_sec % 60
                lrc_timestamp = f"[{mins:02d}:{secs:05.2f}]"
                lrc_line = f"{lrc_timestamp} {text}"

                lrc_lines.append(lrc_line)
                parsed_lines.append({
                    "time_ms": time_ms,
                    "text": text
                })

            raw_lrc = "\n".join(lrc_lines)
            plain_text = "\n".join([p["text"] for p in parsed_lines])

            return {
                "source": "whisper",
                "is_synced": True,
                "language": detected_lang,
                "lines": parsed_lines,
                "raw_lrc": raw_lrc,
                "plain_text": plain_text
            }

        logger.info(f"Iniciando transcrição Whisper para a tarefa {task_id}...")
        result = await loop.run_in_executor(None, _transcribe_sync)

        if on_progress:
            await on_progress({
                "status": "completed",
                "progress": 100.0,
                "message": f"Transcrição concluída ({len(result['lines'])} versos sincronizados)",
                "task_id": task_id,
                "lines_count": len(result["lines"])
            })

        logger.info(f"Transcrição Whisper concluída para {task_id}. {len(result['lines'])} versos gerados.")
        return result


whisper_service = WhisperService()
