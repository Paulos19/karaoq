import os
from pathlib import Path
from typing import List, Union
from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


BASE_DIR = Path(__file__).resolve().parent.parent.parent


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=str(BASE_DIR / ".env"),
        env_file_encoding="utf-8",
        extra="ignore"
    )

    PROJECT_NAME: str = "KaraoQ Backend"
    API_V1_STR: str = "/api/v1"
    DEBUG: bool = True

    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # CORS
    CORS_ORIGINS: Union[str, List[str]] = ["*"]

    @field_validator("CORS_ORIGINS", mode="before")
    @classmethod
    def assemble_cors_origins(cls, v: Union[str, List[str]]) -> List[str]:
        if isinstance(v, str) and not v.startswith("["):
            return [i.strip() for i in v.split(",") if i.strip()]
        elif isinstance(v, list):
            return v
        return ["*"]

    # Storage Settings
    STORAGE_DIR: Path = BASE_DIR / "storage"
    UPLOAD_DIR: Path = BASE_DIR / "storage" / "uploads"
    SEPARATED_DIR: Path = BASE_DIR / "storage" / "separated"

    # Demucs AI Engine Configuration
    DEMUCS_MODEL: str = "htdemucs"
    DEMUCS_TWO_STEMS: str = "vocals"
    DEMUCS_DEVICE: str = "auto"
    DEMUCS_MP3_BITRATE: int = 320

    # Gemini API Key (optional for future lyrics transcription and scoring)
    GEMINI_API_KEY: str = ""

    def setup_directories(self) -> None:
        """Ensure that storage directories exist."""
        self.UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
        self.SEPARATED_DIR.mkdir(parents=True, exist_ok=True)


settings = Settings()
settings.setup_directories()
