import logging
import re
from typing import Any, Dict, List, Optional
import httpx

from app.core.config import settings

logger = logging.getLogger("karaoq.lyrics")


class LyricsService:
    def __init__(self):
        self.client = httpx.AsyncClient(timeout=15.0)

    @staticmethod
    def parse_lrc(lrc_text: str) -> List[Dict[str, Any]]:
        """
        Converte texto em formato LRC ([mm:ss.xx] texto) em uma lista ordenada de versos com timestamps em milissegundos.
        """
        lines_result = []
        time_pattern = re.compile(r"\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]")

        for raw_line in lrc_text.splitlines():
            raw_line = raw_line.strip()
            if not raw_line:
                continue

            # Encontra todas as tags de tempo na linha
            matches = list(time_pattern.finditer(raw_line))
            if not matches:
                continue

            # Remove as tags de tempo para obter o texto limpo do verso
            text = time_pattern.sub("", raw_line).strip()

            for match in matches:
                minutes = int(match.group(1))
                seconds = int(match.group(2))
                fraction_str = match.group(3) or "0"

                # Normaliza frações de segundo para milissegundos
                if len(fraction_str) == 1:
                    millis = int(fraction_str) * 100
                elif len(fraction_str) == 2:
                    millis = int(fraction_str) * 10
                else:
                    millis = int(fraction_str[:3])

                total_ms = (minutes * 60 + seconds) * 1000 + millis
                lines_result.append({
                    "time_ms": total_ms,
                    "text": text
                })

        # Ordena por timestamp
        lines_result.sort(key=lambda x: x["time_ms"])
        return lines_result

    @staticmethod
    def generate_approximate_timestamps(plain_text: str, duration_ms: int = 180000) -> List[Dict[str, Any]]:
        """
        Gera timestamps aproximados uniformes para letras que não possuem marcação LRC de fábrica.
        """
        raw_lines = [line.strip() for line in plain_text.splitlines() if line.strip()]
        if not raw_lines:
            return []

        # Assume 10s de introdução instrumental
        intro_ms = 10000
        available_time = max(duration_ms - intro_ms - 10000, 30000)
        step = available_time / max(len(raw_lines), 1)

        result = []
        for index, line in enumerate(raw_lines):
            time_ms = int(intro_ms + (index * step))
            result.append({
                "time_ms": time_ms,
                "text": line
            })
        return result

    async def search_lyrics(
        self,
        artist: str,
        title: str,
        duration_seconds: Optional[int] = None
    ) -> Dict[str, Any]:
        """
        Busca letras priorizando formato sincronizado (LRC) via LRCLIB e fallback para lyrics-api.
        """
        artist_clean = artist.strip()
        title_clean = title.strip()

        # 1. Tenta buscar no LRCLIB (Serviço Open Source de Letras Sincronizadas)
        try:
            lrclib_url = f"{settings.LRCLIB_API_URL}/get"
            params = {
                "artist_name": artist_clean,
                "track_name": title_clean
            }
            if duration_seconds:
                params["duration"] = str(duration_seconds)

            response = await self.client.get(lrclib_url, params=params)
            if response.status_code == 200:
                data = response.json()
                synced_lrc = data.get("syncedLyrics")
                plain_lyrics = data.get("plainLyrics")

                if synced_lrc:
                    parsed_lines = self.parse_lrc(synced_lrc)
                    if parsed_lines:
                        logger.info(f"Letra sincronizada encontrada no LRCLIB para '{artist_clean} - {title_clean}' ({len(parsed_lines)} versos)")
                        return {
                            "source": "lrclib",
                            "is_synced": True,
                            "artist": data.get("artistName", artist_clean),
                            "title": data.get("trackName", title_clean),
                            "lines": parsed_lines,
                            "raw_lrc": synced_lrc,
                            "plain_text": plain_lyrics or "\n".join([l["text"] for l in parsed_lines])
                        }

                if plain_lyrics:
                    approx_duration = (duration_seconds * 1000) if duration_seconds else 180000
                    lines = self.generate_approximate_timestamps(plain_lyrics, approx_duration)
                    return {
                        "source": "lrclib_plain",
                        "is_synced": False,
                        "artist": data.get("artistName", artist_clean),
                        "title": data.get("trackName", title_clean),
                        "lines": lines,
                        "raw_lrc": None,
                        "plain_text": plain_lyrics
                    }

            # Se não achou na rota direta, tenta rota de busca geral no LRCLIB
            search_url = f"{settings.LRCLIB_API_URL}/search"
            search_res = await self.client.get(search_url, params={"q": f"{artist_clean} {title_clean}"})
            if search_res.status_code == 200:
                items = search_res.json()
                if isinstance(items, list) and len(items) > 0:
                    first = items[0]
                    synced_lrc = first.get("syncedLyrics")
                    plain = first.get("plainLyrics")
                    if synced_lrc:
                        parsed = self.parse_lrc(synced_lrc)
                        if parsed:
                            return {
                                "source": "lrclib_search",
                                "is_synced": True,
                                "artist": first.get("artistName", artist_clean),
                                "title": first.get("trackName", title_clean),
                                "lines": parsed,
                                "raw_lrc": synced_lrc,
                                "plain_text": plain or "\n".join([l["text"] for l in parsed])
                            }
                    if plain:
                        approx_duration = (duration_seconds * 1000) if duration_seconds else 180000
                        return {
                            "source": "lrclib_search_plain",
                            "is_synced": False,
                            "artist": first.get("artistName", artist_clean),
                            "title": first.get("trackName", title_clean),
                            "lines": self.generate_approximate_timestamps(plain, approx_duration),
                            "raw_lrc": None,
                            "plain_text": plain
                        }
        except Exception as ex:
            logger.warning(f"Falha ao consultar LRCLIB: {str(ex)}")

        # 2. Fallback: Consulta na API lyrics-api (YouTube / Musixmatch)
        try:
            urls_to_try = [
                f"{settings.LYRICS_API_URL}/v2/youtube/lyrics",
                f"{settings.LYRICS_API_URL}/v2/musixmatch/lyrics"
            ]
            for endpoint in urls_to_try:
                try:
                    res = await self.client.get(endpoint, params={"title": title_clean, "artist": artist_clean})
                    if res.status_code == 200:
                        json_data = res.json()
                        lyrics_text = json_data.get("lyrics") or json_data.get("data", {}).get("lyrics")
                        if lyrics_text and isinstance(lyrics_text, str) and len(lyrics_text.strip()) > 10:
                            # Verifica se já possui tags LRC
                            if "[" in lyrics_text and "]" in lyrics_text and re.search(r"\[\d{2}:\d{2}", lyrics_text):
                                parsed = self.parse_lrc(lyrics_text)
                                if parsed:
                                    return {
                                        "source": "lyrics-api-synced",
                                        "is_synced": True,
                                        "artist": artist_clean,
                                        "title": title_clean,
                                        "lines": parsed,
                                        "raw_lrc": lyrics_text,
                                        "plain_text": "\n".join([l["text"] for l in parsed])
                                    }

                            approx_duration = (duration_seconds * 1000) if duration_seconds else 180000
                            return {
                                "source": "lyrics-api",
                                "is_synced": False,
                                "artist": artist_clean,
                                "title": title_clean,
                                "lines": self.generate_approximate_timestamps(lyrics_text, approx_duration),
                                "raw_lrc": None,
                                "plain_text": lyrics_text
                            }
                except Exception:
                    continue
        except Exception as ex:
            logger.warning(f"Falha ao consultar lyrics-api: {str(ex)}")

        # Se nenhuma API encontrou
        return {
            "source": "none",
            "is_synced": False,
            "artist": artist_clean,
            "title": title_clean,
            "lines": [],
            "raw_lrc": None,
            "plain_text": ""
        }


lyrics_service = LyricsService()
