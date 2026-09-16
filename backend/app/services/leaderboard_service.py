import json
import logging
import time
import uuid
from pathlib import Path
from typing import Any, Dict, List, Optional

from app.core.config import settings

logger = logging.getLogger("karaoq.leaderboard")


class LeaderboardService:
    def __init__(self):
        self.leaderboards_dir: Path = settings.STORAGE_DIR / "leaderboards"
        self.leaderboards_dir.mkdir(parents=True, exist_ok=True)

    def _get_file(self, song_id: str) -> Path:
        return self.leaderboards_dir / f"{song_id}.json"

    def get_top_scores(self, song_id: str, limit: int = 10) -> List[Dict[str, Any]]:
        """
        Retorna as melhores pontuações de uma música, ordenadas por pontuação decrescente.
        """
        file_path = self._get_file(song_id)
        if not file_path.exists():
            return []

        try:
            with open(file_path, "r", encoding="utf-8") as f:
                entries = json.load(f)
            # Ordena por score decrescente e timestamp mais recente em caso de empate
            entries.sort(key=lambda x: (x.get("score", 0), -x.get("created_at", 0)), reverse=True)
            return entries[:limit]
        except Exception as ex:
            logger.error(f"Erro ao carregar leaderboard de {song_id}: {ex}")
            return []

    def submit_score(
        self,
        song_id: str,
        singer_name: str,
        score: int,
        rank: str,
        max_combo: int,
        perfect_hits: int = 0
    ) -> Dict[str, Any]:
        """
        Registra uma nova pontuação no placar da música.
        """
        file_path = self._get_file(song_id)
        entries = []
        if file_path.exists():
            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    entries = json.load(f)
            except Exception as ex:
                logger.warning(f"Falha ao ler leaderboard existente de {song_id}: {ex}")
                entries = []

        clean_name = singer_name.strip()
        if not clean_name:
            clean_name = "Cantor Anônimo"

        entry = {
            "id": str(uuid.uuid4()),
            "song_id": song_id,
            "singer_name": clean_name[:30],
            "score": max(0, min(10000, score)),
            "rank": rank,
            "max_combo": max_combo,
            "perfect_hits": perfect_hits,
            "created_at": time.time()
        }

        entries.append(entry)

        # Salva o arquivo atualizado
        try:
            with open(file_path, "w", encoding="utf-8") as f:
                json.dump(entries, f, ensure_ascii=False, indent=2)
            logger.info(f"Nova pontuação registrada para música {song_id}: {clean_name} fez {score} pts (Rank {rank})")
        except Exception as ex:
            logger.error(f"Erro ao salvar entrada no leaderboard de {song_id}: {ex}")

        return entry


leaderboard_service = LeaderboardService()
