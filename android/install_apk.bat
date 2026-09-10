@echo off
setlocal enabledelayedexpansion

echo =========================================================
echo   KaraoQ - Build & Instalação Direta via Depuração USB
echo =========================================================

set ADB_PATH=C:\Users\Usuario\AppData\Local\Android\Sdk\platform-tools\adb.exe

if not exist "!ADB_PATH!" (
    echo [!] ADB não encontrado no caminho padrão. Tentando 'adb' do PATH...
    set ADB_PATH=adb
)

echo.
echo [1/4] Verificando dispositivo conectado via USB...
"!ADB_PATH!" devices

echo.
echo [2/4] Redirecionando porta 8000 para acesso direto ao backend local (adb reverse)...
"!ADB_PATH!" reverse tcp:8000 tcp:8000

echo.
echo [3/4] Compilando APK de depuração (assembleDebug)...
cd /d "%~dp0"
call gradlew.bat assembleDebug

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERRO] Falha na compilação do APK. Verifique os erros acima.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [4/4] Instalando e iniciando o KaraoQ no celular...
"!ADB_PATH!" install -r app\build\outputs\apk\debug\app-debug.apk
"!ADB_PATH!" shell am start -n com.karaoq.app.debug/com.karaoq.app.MainActivity

echo.
echo =========================================================
echo   Sucesso! KaraoQ instalado e iniciado no aparelho!
echo =========================================================
pause
