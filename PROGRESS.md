# PROGRESS.md - KaraoQ Development Roadmap & Status

Acompanhamento em tempo real do desenvolvimento do KaraoQ (Backend IA + Android Mobile + Infraestrutura).

---

## 📊 Status Geral do Projeto

- **Fase Atual:** Fase 1 - Setup Arquitetural & Fundação Monorepo (Concluída)
- **Última Atualização:** 10/09/2026
- **Repositório Remoto:** `https://github.com/Paulos19/karaoq.git`
- **URL da API em Produção (Easypanel):** `https://services-karaoq.khdya3.easypanel.host/`

---

## 🗺️ Roadmap & Tarefas

### 1. Documentação & Governança Monorepo
- [x] Criação e detalhamento das diretrizes arquiteturais em `AGENT.md`
- [x] Criação do arquivo de progresso contínuo `PROGRESS.md`
- [x] Configuração do `.gitignore` abrangente (Android, Python, Storage, Gradle, OS)
- [x] Inicialização do repositório Git e push para `origin/main` (`https://github.com/Paulos19/karaoq.git`)

### 2. Backend de IA (`/backend`)
- [x] Criação do `requirements.txt` com FastAPI, Demucs, PyTorch, Uvicorn e utilitários
- [x] Configuração de ambiente e variáveis centrais em `backend/app/core/config.py`
- [x] Implementação do serviço de separação de stems (`backend/app/services/demucs_service.py` com `htdemucs --two-stems=vocals`)
- [x] Implementação da API REST v1 (`POST /api/v1/separate`, `GET /api/v1/separate/{task_id}`, download de stems)
- [x] Aplicação principal FastAPI em `backend/app/main.py` com CORS, rotas estáticas e healthcheck
- [x] Estrutura de storage local (`storage/uploads/` e `storage/separated/`)
- [x] Criação de arquivo `.env.example` com template de configurações

### 3. Deploy & VPS (Easypanel)
- [x] `Dockerfile` otimizado para Debian com FFmpeg, PyTorch, Demucs e Uvicorn
- [x] `docker-compose.yml` para fácil implantação no Easypanel com volumes persistentes para `/storage`
- [x] Script de execução rápida local `backend/run_local.bat`

### 4. Cliente Mobile Android (`/android`)
- [x] Configuração do Gradle raiz (`settings.gradle.kts` e `build.gradle.kts`)
- [x] Configuração do módulo `:app` com Jetpack Compose BOM, Material 3 e Android SDK 34
- [x] Adição das dependências: Media3 (ExoPlayer), Retrofit 2 + OkHttp 4, ViewModel, Coroutines
- [x] `AndroidManifest.xml` com permissões de internet, áudio e cleartext traffic para depuração
- [x] Camada de Dados (`data/model/SeparationModels.kt`, `data/remote/KaraoqApiService.kt`, `data/remote/ApiClient.kt`)
- [x] Camada de Domínio (`domain/model/AudioModels.kt`)
- [x] Camada de Apresentação (`presentation/ui/theme/*`, `presentation/home/HomeScreen.kt`, `HomeViewModel.kt`)
- [x] `MainActivity.kt` integrando o fluxo de envio de áudio, acompanhamento de progresso e player
- [x] Script para compilação e instalação direta via depuração USB (`android/install_apk.bat` com `adb reverse`)
- [x] Compilação do APK de Debug com Sucesso: `app-debug.apk` gerado (19.5 MB)
- [x] Instalação e execução com sucesso em dispositivo físico via ADB (modelo `2412DPC0AG`) com `adb reverse` ativo

### 5. Próximos Passos (Fases Futuras)
- [ ] Sincronização de letras LRC / AI Lyrics Transcription
- [ ] Sistema de pontuação vocal em tempo real (Pitch Detection / AudioRecord)
- [ ] Suporte a WebSocket para progresso de inferência em tempo real
