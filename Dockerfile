# ==========================================
# KaraoQ Backend - Root Dockerfile (Easypanel / VPS)
# ==========================================
FROM python:3.11-slim-bookworm

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PORT=8000

# Instala dependências de sistema essenciais (FFmpeg para áudio, libsndfile, curl)
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    libsndfile1 \
    curl \
    ca-certificates \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copia e instala dependências Python a partir da raiz do monorepo
COPY backend/requirements.txt requirements.txt

# Otimização: Instala torch/torchaudio CPU para economizar espaço e agilizar o deploy na VPS
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir torch torchaudio --index-url https://download.pytorch.org/whl/cpu && \
    pip install --no-cache-dir -r requirements.txt

# Pre-baixa o modelo htdemucs durante o build para que a primeira requisição seja imediata
RUN python3 -c "import demucs.pretrained; demucs.pretrained.get_model('htdemucs')" || true

# Copia o código da aplicação
COPY backend/app ./app

# Cria estrutura de pastas de storage
RUN mkdir -p storage/uploads storage/separated

EXPOSE 8000

# Execução do servidor Uvicorn
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
