import logging
import os
from pathlib import Path
from typing import Any, Dict, List, Optional

from app.core.config import settings
from app.services.lyrics_service import lyrics_service

logger = logging.getLogger("karaoq.ai_transcription")


class AiTranscriptionService:
    def __init__(self):
        self.api_key = settings.GEMINI_API_KEY

    def is_configured(self) -> bool:
        return bool(self.api_key or os.environ.get("GEMINI_API_KEY"))

    async def transcribe_vocals_to_lrc(self, task_id: str) -> Dict[str, Any]:
        """
        Transcreve a faixa isolada de voz ('vocals.mp3') em letra sincronizada LRC
        utilizando o modelo de áudio do Google Gemini.
        """
        key = self.api_key or os.environ.get("GEMINI_API_KEY")
        if not key:
            raise ValueError(
                "Chave GEMINI_API_KEY não configurada no ambiente. "
                "Adicione a variável de ambiente no Easypanel ou no arquivo .env."
            )

        vocals_path = settings.SEPARATED_DIR / task_id / "vocals.mp3"
        if not vocals_path.exists():
            raise FileNotFoundError(f"Arquivo de voz isolada não encontrado em {vocals_path}")

        try:
            from google import genai
        except ImportError:
            raise ImportError(
                "O pacote 'google-genai' não está instalado no ambiente Python. "
                "Instale via 'pip install google-genai'."
            )

        logger.info(f"Iniciando transcrição de voz com IA para a tarefa {task_id}...")

        client = genai.Client(api_key=key)

        uploaded_audio = None
        try:
            # Envia o arquivo de áudio para a API do Gemini
            uploaded_audio = client.files.upload(file=str(vocals_path))
            logger.info(f"Áudio enviado ao Gemini. Arquivo remoto: {uploaded_audio.name}")

            prompt = (
                "Você é um engenheiro de áudio e transcreve letras de música com sincronização precisa. "
                "Ouça a faixa de vocal isolado enviada e transcreva a letra no formato LRC padrão "
                "com timestamps no formato [mm:ss.xx] para cada verso cantado. "
                "Importante: Responda estritamente com as linhas no formato LRC (ex: [00:15.20] Verso aqui). "
                "Não adicione comentários, introduções ou markdown fences (como ```lrc)."
            )

            response = client.models.generate_content(
                model="gemini-2.5-flash",
                contents=[uploaded_audio, prompt]
            )

            raw_lrc = response.text.strip() if response.text else ""

            # Remove eventuais fences de código markdown se o modelo incluir
            if raw_lrc.startswith("```"):
                raw_lrc = raw_lrc.strip("`")
                if raw_lrc.startswith("lrc"):
                    raw_lrc = raw_lrc[3:].strip()

            # Processa o formato LRC em versos com timestamps
            lines = lyrics_service.parse_lrc(raw_lrc)
            plain_text = "\n".join([line["text"] for line in lines if line["text"].strip()])

            logger.info(f"Transcrição concluída para a tarefa {task_id}. {len(lines)} versos sincronizados gerados.")

            return {
                "source": "gemini_ai",
                "is_synced": True,
                "lines": lines,
                "raw_lrc": raw_lrc,
                "plain_text": plain_text
            }

        except Exception as ex:
            logger.error(f"Falha na transcrição com Gemini para a tarefa {task_id}: {ex}", exc_info=True)
            raise RuntimeError(f"Erro na transcrição de voz com IA: {str(ex)}")

        finally:
            # Limpa o arquivo temporário na nuvem do Gemini
            if uploaded_audio and hasattr(uploaded_audio, "name"):
                try:
                    client.files.delete(name=uploaded_audio.name)
                except Exception as del_err:
                    logger.warning(f"Erro ao remover arquivo temporário do Gemini: {del_err}")


ai_transcription_service = AiTranscriptionService()
