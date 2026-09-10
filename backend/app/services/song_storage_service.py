import json
import logging
import shutil
import time
from pathlib import Path
from typing import Any, Dict, List, Optional

from app.core.config import settings

logger = logging.getLogger("karaoq.songs")


class SongStorageService:
    def __init__(self):
        self.songs_dir: Path = settings.SONGS_DIR
        self.songs_dir.mkdir(parents=True, exist_ok=True)

    def list_songs(self) -> List[Dict[str, Any]]:
        """
        Retorna todas as músicas salvas no storage, ordenadas pela mais recente.
        """
        songs = []
        if not self.songs_dir.exists():
            return songs

        for item in self.songs_dir.iterdir():
            if item.is_dir():
                meta_file = item / "metadata.json"
                if meta_file.exists():
                    try:
                        with open(meta_file, "r", encoding="utf-8") as f:
                            data = json.load(f)
                            songs.append(data)
                    except Exception as ex:
                        logger.warning(f"Erro ao ler metadata de {item.name}: {ex}")

        # Ordena pela data de criação decrescente
        songs.sort(key=lambda x: x.get("created_at", 0), reverse=True)
        return songs

    def get_song(self, song_id: str) -> Optional[Dict[str, Any]]:
        """
        Recupera os detalhes de uma música salva.
        """
        meta_file = self.songs_dir / song_id / "metadata.json"
        if not meta_file.exists():
            return None
        try:
            with open(meta_file, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as ex:
            logger.error(f"Erro ao ler metadata da música {song_id}: {ex}")
            return None

    def save_song(self, song_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Salva ou atualiza os metadados de uma música no storage.
        """
        song_id = song_data.get("id")
        if not song_id:
            raise ValueError("ID da música é obrigatório.")

        song_folder = self.songs_dir / song_id
        song_folder.mkdir(parents=True, exist_ok=True)

        meta_file = song_folder / "metadata.json"

        # Garante timestamps
        if "created_at" not in song_data:
            song_data["created_at"] = time.time()
        song_data["updated_at"] = time.time()

        with open(meta_file, "w", encoding="utf-8") as f:
            json.dump(song_data, f, ensure_ascii=False, indent=2)

        logger.info(f"Música {song_id} ('{song_data.get('title')}') salva no storage com sucesso.")
        return song_data

    def delete_song(self, song_id: str) -> bool:
        """
        Remove os metadados e áudios salvos da música.
        """
        song_folder = self.songs_dir / song_id
        if song_folder.exists():
            shutil.rmtree(song_folder, ignore_errors=True)

        # Opcionalmente remove a pasta de stems gerada
        stems_folder = settings.SEPARATED_DIR / song_id
        if stems_folder.exists():
            shutil.rmtree(stems_folder, ignore_errors=True)

        logger.info(f"Música {song_id} removida do storage.")
        return True


song_storage_service = SongStorageService()
