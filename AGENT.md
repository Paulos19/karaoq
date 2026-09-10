# AGENT.md - KaraoQ Architecture & Guidelines

## 1. Project Overview
- **Name:** KaraoQ
- **Description:** A full-stack mobile karaoke ecosystem with AI-powered stem separation (vocal vs. instrumental isolation), lyrics synchronization, audio playback, and real-time vocal scoring.
- **Repository Type:** Monorepo containing both the Android mobile client and the Python inference backend.

---

## 2. Directory Structure & Monorepo Layout
```text
karaoq/
├── AGENT.md
├── PROGRESS.md
├── .gitignore
├── docker-compose.yml
├── backend/
│   ├── app/
│   │   ├── api/
│   │   │   └── v1/
│   │   │       ├── endpoints/
│   │   │       │   └── separate.py
│   │   │       └── api.py
│   │   ├── core/
│   │   │   └── config.py
│   │   ├── services/
│   │   │   └── demucs_service.py
│   │   └── main.py
│   ├── storage/
│   │   ├── uploads/
│   │   └── separated/
│   ├── requirements.txt
│   ├── Dockerfile
│   └── .env.example
└── android/
    ├── app/
    │   ├── src/main/
    │   │   ├── java/com/karaoq/app/
    │   │   │   ├── data/
    │   │   │   │   ├── model/
    │   │   │   │   └── remote/
    │   │   │   ├── domain/
    │   │   │   │   └── model/
    │   │   │   ├── presentation/
    │   │   │   │   ├── home/
    │   │   │   │   └── ui/theme/
    │   │   │   └── MainActivity.kt
    │   │   ├── res/
    │   │   └── AndroidManifest.xml
    │   └── build.gradle.kts
    ├── gradle/wrapper/
    │   ├── gradle-wrapper.jar
    │   └── gradle-wrapper.properties
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── gradlew.bat
```

---

## 3. Technology Stack

### Backend (`/backend`)
- **Language:** Python 3.10+
- **Framework:** FastAPI + Uvicorn
- **AI / Audio Separation Engine:** `demucs` (Meta `htdemucs` model with `--two-stems=vocals`)
- **Audio Processing Utilities:** `torchaudio`, `soundfile`, `FFmpeg`
- **Network / Transport:** REST API + Async Background Tasks (polling or WebSocket status tracking)
- **Deployment:** Docker / Docker Compose ready for VPS with Easypanel

### Mobile Client (`/android`)
- **Language:** Kotlin (Latest stable)
- **UI Toolkit:** Jetpack Compose + Material 3
- **Architecture:** Clean Architecture + MVVM / MVI with StateFlow & Coroutines
- **Audio Playback:** AndroidX Media3 (ExoPlayer)
- **Networking:** Retrofit 2 + OkHttp 4 (multipart audio upload with progress tracking)
- **Build System:** Gradle Kotlin DSL (`build.gradle.kts`) with Android SDK 34
- **Testing & Debugging:** USB Debugging via ADB (`assembleDebug` / `installDebug`)
- **Audio Engine (Future):** Oboe (C++/NDK) or AudioRecord for low-latency voice capture and pitch tracking.

---

## 4. Key Rules for the Agent
1. **Separation of Concerns:** Keep `/backend` and `/android` completely decoupled in terms of business logic.
2. **Audio Performance First:** Audio stem generation must always prioritize the `htdemucs` 2-stems mode (`vocals` and `no_vocals`) to save memory and inference time.
3. **Reactive Mobile UI:** All Android UI must be written in Jetpack Compose, handling loading, uploading, processing, and error states gracefully.
4. **USB Debugging Ready:** Android app must be configured with internet and cleartext permissions for seamless local testing via USB debugging and ADB.
5. **No Code Hallucinations:** When generating code, provide complete, syntactically correct files with exact dependencies and imports.
6. **Context Retention:** Always check `AGENT.md` before creating new modules, endpoints, or screens to ensure architectural consistency.
7. **Progress Tracking:** Maintain `PROGRESS.md` constantly updated so the developer can follow the roadmap and real-time status.