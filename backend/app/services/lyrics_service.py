import logging
import os
import re
from typing import Any, Dict, List, Optional
import httpx

from app.core.config import settings

logger = logging.getLogger("karaoq.lyrics")


class LyricsService:
    def __init__(self):
        self.client = httpx.AsyncClient(timeout=15.0)

    @staticmethod
    def clean_metadata(text: str) -> str:
        """
        Remove ruídos comuns de títulos de áudio/vídeo (ex: '(Music Video)', '[Official Audio]', etc.)
        para aumentar drasticamente a taxa de assertividade nas buscas de letras.
        """
        if not text:
            return ""
        # Remove conteúdo entre parênteses, colchetes ou chaves
        cleaned = re.sub(r"\s*[\(\[\{].*?[\)\]\}]", "", text)
        # Remove termos comuns caso não estejam entre parênteses
        cleaned = re.sub(
            r"(?i)\b(official\s+video|official\s+audio|music\s+video|clipe\s+oficial|video\s+oficial|lyric\s+video|remastered|hd|4k)\b",
            "",
            cleaned
        )
        cleaned = re.sub(r"\s+", " ", cleaned).strip()
        return cleaned or text.strip()

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

            matches = list(time_pattern.finditer(raw_line))
            if not matches:
                continue

            text = time_pattern.sub("", raw_line).strip()

            for match in matches:
                minutes = int(match.group(1))
                seconds = int(match.group(2))
                fraction_str = match.group(3) or "0"

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

    async def search_lyricfind(self, artist: str, title: str) -> Optional[Dict[str, Any]]:
        """
        Consulta o serviço oficial LyricFind Web Service (lyric.do com formato LRC):
        https://docs.lyricfind.com/?key=eyJkb2N1bWVudCI6ImZ0cCIsIm9wdGlvbnMiOnsiZmVlZCI6Im5ld0ZUUCIsInZlcnNpb24iOiJscmMifX0=
        """
        api_key = settings.LYRICFIND_API_KEY or os.environ.get("LYRICFIND_API_KEY", "")
        if not api_key:
            return None

        lrc_key = settings.LYRICFIND_LRC_KEY or os.environ.get("LYRICFIND_LRC_KEY", "")
        display_key = settings.LYRICFIND_DISPLAY_KEY or os.environ.get("LYRICFIND_DISPLAY_KEY", "")

        url = f"{settings.LYRICFIND_API_URL.rstrip('/')}/lyric.do"

        # Formata identificadores conforme convenção LyricFind (dois-pontos e vírgulas são caracteres reservados)
        safe_artist = artist.replace(":", ";").replace(",", " ").strip()
        safe_title = title.replace(":", ";").replace(",", " ").strip()

        params = {
            "apikey": api_key,
            "reqtype": "default",
            "trackid": f"artistname:{safe_artist},trackname:{safe_title}",
            "format": "lrc",
            "output": "json",
            "useragent": "KaraoQ/1.0"
        }
        if lrc_key:
            params["lrckey"] = lrc_key
        if display_key:
            params["displaykey"] = display_key

        try:
            res = await self.client.get(url, params=params)
            if res.status_code == 200:
                data = res.json()
                resp_code = data.get("response", {}).get("code")

                # Códigos 100, 101, 111 indicam sucesso na recuperação da letra/LRC
                if resp_code in (100, 101, 111):
                    track = data.get("track", {})
                    lrc_items = track.get("lrc")

                    if lrc_items and isinstance(lrc_items, list):
                        lrc_lines = []
                        parsed_lines = []
                        for item in lrc_items:
                            text = item.get("line", "").strip()
                            if not text:
                                continue

                            millis = int(item.get("milliseconds") or 0)
                            ts = item.get("lrc_timestamp")
                            if not ts:
                                mins = millis // 60000
                                secs = (millis % 60000) / 1000.0
                                ts = f"[{mins:02d}:{secs:05.2f}]"

                            lrc_lines.append(f"{ts} {text}")
                            parsed_lines.append({
                                "time_ms": millis,
                                "text": text
                            })

                        if parsed_lines:
                            logger.info(f"Letra LRC sincronizada encontrada no LyricFind para '{artist} - {title}' ({len(parsed_lines)} versos)")
                            raw_lrc = "\n".join(lrc_lines)
                            artist_name = track.get("artist", {}).get("name", artist) if isinstance(track.get("artist"), dict) else artist
                            return {
                                "source": "lyricfind",
                                "is_synced": True,
                                "artist": artist_name,
                                "title": track.get("title", title),
                                "lines": parsed_lines,
                                "raw_lrc": raw_lrc,
                                "plain_text": track.get("lyrics") or "\n".join([l["text"] for l in parsed_lines])
                            }

                    plain = track.get("lyrics")
                    if plain:
                        duration_ms = 180000
                        return {
                            "source": "lyricfind_plain",
                            "is_synced": False,
                            "artist": artist,
                            "title": track.get("title", title),
                            "lines": self.generate_approximate_timestamps(plain, duration_ms),
                            "raw_lrc": None,
                            "plain_text": plain
                        }
        except Exception as ex:
            logger.warning(f"Falha ao consultar LyricFind: {str(ex)}")

        return None

    async def search_lyrics(
        self,
        artist: str,
        title: str,
        duration_seconds: Optional[int] = None
    ) -> Dict[str, Any]:
        """
        Busca letras sincronizadas priorizando LyricFind e LRCLIB com sanitização inteligente de títulos.
        """
        artist_raw = artist.strip()
        title_raw = title.strip()

        artist_clean = self.clean_metadata(artist_raw)
        title_clean = self.clean_metadata(title_raw)

        # 1. Tenta LyricFind (com chave de API se configurada)
        lyricfind_result = await self.search_lyricfind(artist_clean, title_clean)
        if not lyricfind_result and (artist_clean != artist_raw or title_clean != title_raw):
            lyricfind_result = await self.search_lyricfind(artist_raw, title_raw)

        if lyricfind_result and lyricfind_result.get("lines"):
            return lyricfind_result

        # 2. Tenta LRCLIB (Serviço Open Source de Letras Sincronizadas)
        queries_to_try = [
            (artist_clean, title_clean),
        ]
        if artist_clean != artist_raw or title_clean != title_raw:
            queries_to_try.append((artist_raw, title_raw))

        for art, tit in queries_to_try:
            try:
                lrclib_url = f"{settings.LRCLIB_API_URL}/get"
                params = {
                    "artist_name": art,
                    "track_name": tit
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
                            logger.info(f"Letra sincronizada encontrada no LRCLIB para '{art} - {tit}' ({len(parsed_lines)} versos)")
                            return {
                                "source": "lrclib",
                                "is_synced": True,
                                "artist": data.get("artistName", art),
                                "title": data.get("trackName", tit),
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
                            "artist": data.get("artistName", art),
                            "title": data.get("trackName", tit),
                            "lines": lines,
                            "raw_lrc": None,
                            "plain_text": plain_lyrics
                        }

                # Se a busca direta falhar, tenta rota de busca geral por texto
                search_url = f"{settings.LRCLIB_API_URL}/search"
                search_res = await self.client.get(search_url, params={"q": f"{art} {tit}"})
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
                                    "artist": first.get("artistName", art),
                                    "title": first.get("trackName", tit),
                                    "lines": parsed,
                                    "raw_lrc": synced_lrc,
                                    "plain_text": plain or "\n".join([l["text"] for l in parsed])
                                }
                        if plain:
                            approx_duration = (duration_seconds * 1000) if duration_seconds else 180000
                            return {
                                "source": "lrclib_search_plain",
                                "is_synced": False,
                                "artist": first.get("artistName", art),
                                "title": first.get("trackName", tit),
                                "lines": self.generate_approximate_timestamps(plain, approx_duration),
                                "raw_lrc": None,
                                "plain_text": plain
                            }
            except Exception as ex:
                logger.warning(f"Falha ao consultar LRCLIB para '{art} - {tit}': {str(ex)}")

        # 3. Fallback: Consulta na API de letras do YouTube (removendo a rota quebrada musixmatch)
        try:
            youtube_url = f"{settings.LYRICS_API_URL}/v2/youtube/lyrics"
            res = await self.client.get(youtube_url, params={"title": title_clean, "artist": artist_clean})
            if res.status_code == 200:
                json_data = res.json()
                lyrics_text = json_data.get("lyrics") or json_data.get("data", {}).get("lyrics")
                if lyrics_text and isinstance(lyrics_text, str) and len(lyrics_text.strip()) > 10:
                    if "[" in lyrics_text and "]" in lyrics_text and re.search(r"\[\d{2}:\d{2}", lyrics_text):
                        parsed = self.parse_lrc(lyrics_text)
                        if parsed:
                            return {
                                "source": "youtube_synced",
                                "is_synced": True,
                                "artist": artist_clean,
                                "title": title_clean,
                                "lines": parsed,
                                "raw_lrc": lyrics_text,
                                "plain_text": "\n".join([l["text"] for l in parsed])
                            }

                    approx_duration = (duration_seconds * 1000) if duration_seconds else 180000
                    return {
                        "source": "youtube_plain",
                        "is_synced": False,
                        "artist": artist_clean,
                        "title": title_clean,
                        "lines": self.generate_approximate_timestamps(lyrics_text, approx_duration),
                        "raw_lrc": None,
                        "plain_text": lyrics_text
                    }
        except Exception as ex:
            logger.warning(f"Falha ao consultar lyrics-api youtube: {str(ex)}")

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

