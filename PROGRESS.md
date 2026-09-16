# PROGRESS.md - KaraoQ Development Roadmap & Status

Acompanhamento em tempo real do desenvolvimento do KaraoQ (Backend IA + Android Mobile + Infraestrutura).

---

## 📊 Status Geral do Projeto

- **Fase Atual:** Fase 7 - Pitch Detection, Pontuação & WebSockets (Concluída)
- **Última Atualização:** 16/09/2026
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

### 5. Letras Sincronizadas & Biblioteca no Storage
- [x] Serviço de busca de letras (`backend/app/services/lyrics_service.py`) com integração LRCLIB + `lyrics-api`
- [x] Parser de formato LRC com timestamps precisos em milissegundos
- [x] Serviço de armazenamento e persistência de músicas (`backend/app/services/song_storage_service.py` em `storage/songs/`)
- [x] Endpoints de API REST: `GET /api/v1/lyrics/search`, `GET /api/v1/songs`, `POST /api/v1/songs`, `DELETE /api/v1/songs/{id}`
- [x] Componente de Karaokê com Teleprompter em tempo real (`presentation/components/LyricsView.kt` com auto-scroll e seek interativo)
- [x] Tela de biblioteca de músicas salvas (`presentation/library/SavedSongsScreen.kt`) com reprodução instantânea
- [x] Interface com abas de navegação ("Criar Karaokê" e "Músicas Salvas") e campos de Cantor e Título

### 6. Modo Palco Karaokê & Captação de Áudio (Concluída no App)
- [x] Gerenciador nativo de microfone (`data/audio/MicrophoneManager.kt`) com `AudioRecord` (PCM 16-bit, 44.1kHz), cálculo de amplitude RMS e suavização
- [x] Tela dedicada do Palco Karaokê (`presentation/karaoke/KaraokeScreen.kt`) com visual moderno e imersivo
- [x] Contagem regressiva animada de 5 segundos antes do início do playback para preparação do cantor
- [x] Visualizador VU Meter sonoro reativo com barras equalizadoras alimentadas pela voz do usuário
- [x] Seletor instantâneo entre "Voz Guia (Lead Vocal)" e "Playback Puro"
- [x] Botão de ação "Cantar" direto no cartão de músicas salvas da biblioteca (`SavedSongsScreen.kt`)
- [x] Gerenciamento reativo de permissão `RECORD_AUDIO` em tempo de execução via Jetpack Compose

### 7. Pitch Detection Vocal, Pontuação de Karaokê & WebSockets (Concluída)
- [x] Implementação do algoritmo YIN de detecção de afinação em tempo real (`data/audio/PitchDetector.kt`) com conversão para notas musicais (C4, A4, etc.) e medição de desvio em cents
- [x] Integração do detector de afinação ao fluxo de captura de áudio (`data/audio/MicrophoneManager.kt`)
- [x] Motor de pontuação de Karaokê (`domain/audio/KaraokeScoringEngine.kt`) com avaliação de estabilidade, combos (x1 a x4) e feedbacks instantâneos ("Perfeito!", "Muito Bom!", "Quase lá!")
- [x] Afinador visual e placar dinâmico em tempo real na tela do palco (`presentation/karaoke/KaraokeScreen.kt`)
- [x] Modal de Fim de Show com pontuação clássica (0 a 10.000 pts), estrelas (1 a 5), troféus e sistema de ranking (Rank S, A, B, C)
- [x] Canal WebSocket no backend FastAPI (`/api/v1/separate/ws/{task_id}`) com broadcasting em tempo real do progresso da separação Demucs
- [x] Cliente WebSocket no app Android (`data/remote/SeparationWebSocketManager.kt`) integrado ao ViewModel com fallback transparente para polling HTTP

### 8. Próximos Passos (Fases Futuras)
- [ ] Gravação e salvamento da performance vocal mixada com o instrumental para compartilhamento
- [ ] Transcrição de letras de músicas desconhecidas com IA (Whisper / Gemini Audio)
- [ ] Placar global de líderes e ranking entre usuários na nuvem

