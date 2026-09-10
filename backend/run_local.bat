@echo off
echo =========================================================
echo   KaraoQ - Iniciando Servidor FastAPI + Demucs Local
echo =========================================================

cd /d "%~dp0"

if exist ".venv\Scripts\activate.bat" (
    echo Ativando ambiente virtual .venv...
    call .venv\Scripts\activate.bat
) else (
    echo Verificando dependencias globais...
    pip install -r requirements.txt
)

echo.
echo Iniciando servidor Uvicorn em http://0.0.0.0:8000 ...
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
