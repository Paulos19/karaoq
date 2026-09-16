# 🎤 KaraoQ - Análise do Produto, Proposta de Valor & Catálogo de Funcionalidades

---

## 1. Visão Geral & Proposta de Valor

O **KaraoQ** é um ecossistema completo de karaokê inteligente que combina **Inteligência Artificial de ponta** para processamento de áudio com uma experiência mobile imersiva, moderna e de alta performance.

### A Dor do Usuário
Tradicionalmente, para cantar uma música em karaokê, o usuário precisa depender de versões instrumentais pré-gravadas no YouTube ou em plataformas específicas, que frequentemente têm qualidade duvidosa, tom incompatível ou ausência de letras sincronizadas em tempo real. Além disso, plataformas convencionais raramente oferecem pontuação vocal interativa com afinação real (pitch detection).

### A Proposta do KaraoQ
O KaraoQ elimina qualquer intermediário: **o usuário envia qualquer arquivo de áudio ou música comercial** (MP3, WAV, M4A, FLAC, OGG), e a plataforma cuida de todo o pipeline em segundos:
1. **Isola o vocal original e o instrumental** com fidelidade de estúdio via IA (Demucs).
2. **Localiza ou transcreve a letra com marcação milimétrica (LRC)** usando bases abertas, provedores comerciais (LyricFind) ou IA local (Whisper).
3. **Oferece um palco interativo completo** no smartphone com teleprompter sincronizado, avaliação de afinação vocal em tempo real (algoritmo YIN), pontuação estilo arcade, gravação da performance e placar global de líderes (Leaderboard).

---

## 2. Arquitetura do Sistema

O KaraoQ é arquitetado como um **monorepo desacoplado**, separando o processamento pesado de IA (servidor) da experiência interativa de baixa latência (cliente Android):

```
┌──────────────────────────────────────────────────────────┐
│                    CLIENTE ANDROID                       │
│  - Jetpack Compose + Material 3 (Dark Cyber Theme)       │
│  - Media3 ExoPlayer (Streaming & Alternância de Stems)   │
│  - AudioRecord PCM (44.1kHz / 16-bit)                    │
│  - Detector YIN de Pitch Vocal (Hz, Nota, Cents)         │
│  - Motor de Pontuação de Karaokê (0 a 10.000 pts)        │
│  - Gravador de Performance (WAV) & Share Intent          │
│  - WebSockets com Fallback Transparente para HTTP        │
└─────────────────────────────┬────────────────────────────┘
                              │ HTTP REST / WebSockets
┌─────────────────────────────▼────────────────────────────┐
│                    BACKEND FASTAPI                       │
│  - Separação Demucs (htdemucs 2-stems, WAV/MP3 320kbps)  │
│  - Transcrição Local Faster-Whisper (INT8 Quantized)     │
│  - Transcrição Fallback Gemini Audio (Multimodal)        │
│  - Busca de Letras: LyricFind + LRCLIB + YouTube Lyrics  │
│  - Sanitização Inteligente de Títulos (clean_metadata)   │
│  - Storage de Áudios & Músicas Salvas (/storage/)        │
│  - Leaderboard Global por Música                         │
└──────────────────────────────────────────────────────────┘
```

---

## 3. Catálogo Completo de Funcionalidades (Features)

### 3.1. Separação de Áudio com Inteligência Artificial (Stems)
- **Modelo de Separação:** Utiliza o modelo de redes neurais profundas **Demucs (`htdemucs`)** da Meta, com arquitetura Transformer híbrida treinada especificamente para separação musical em 2 faixas (`vocals` e `no_vocals`).
- **Codificação de Alta Qualidade:** Exportação direta em MP3 320kbps de alta fidelidade com fallback transparente para encoder PCM WAV e conversão otimizada via `lameenc`.
- **Prevenção de Quedas de Memória (OOM Safe):** Segmentação nativa de 7.8 segundos e execução controlada em single-thread (`-j 1`) para suportar músicas completas (3 a 6 minutos) mesmo em servidores VPS com recursos limitados.
- **Transmissão em Tempo Real:** Streaming contínuo da barra de progresso do Demucs (`0% a 100%`) via WebSocket a cada 1.5s, prevenindo desconexões por ociosidade (idle timeout) em proxies reversos (Easypanel, Traefik, Nginx).
- **Validação Estrita de Integridade:** Validação automática de tamanho e existência física dos arquivos gerados, impedindo criação de faixas corrompidas ou com 0 bytes.

---

### 3.2. Mecanismo de Letras Sincronizadas & Transcrição por IA
- **Algoritmo de Sanitização de Metadados (`clean_metadata`):** Remove ruídos de nomes de arquivos como `(Official Music Video)`, `[Clipe Oficial]`, `Remastered`, `HD`, etc., aumentando drasticamente a taxa de assertividade na busca.
- **Cadeia de Busca Multi-Provedores:**
  1. **LyricFind Web Service:** Integração oficial com a API comercial LyricFind (`api.lyricfind.com/lyric.do`) com suporte a formato `lrc` e mapeamento de tempo em milissegundos.
  2. **LRCLIB Database:** Consulta prioritária à base aberta LRCLIB com busca exata e busca fonética geral por Artista e Título.
  3. **YouTube Lyrics Fallback:** Consulta de letras em provedores alternativos para cobertura de faixas raras.
- **Transcrição Vocal com IA Local (Whisper):**
  - Quando a música não possui letra sincronizada na internet, o KaraoQ utiliza o **Faster-Whisper** com quantização `int8` diretamente na CPU do backend para transcrever o áudio isolado da voz (`vocals.mp3`).
  - Gera timestamps exatos de início e término de cada verso em formato padrão LRC (`[mm:ss.xx]`), transmitindo versos em tempo real via WebSocket para o app.
- **Fallback Gemini Audio:** Suporte a transcrição multimodal avançada via Google Gemini Audio para processamento em nuvem.

---

### 3.3. Palco Karaokê & Teleprompter Interativo
- **Visual Dark Moderno ("Cyber Stage"):** Interface imersiva construída com Jetpack Compose utilizando paleta exclusiva (Neon Cyan `#00F5D4`, Electric Green `#00FF88`, Neon Pink `#FF007F`, Neon Purple `#7B2CBF` e Dark Background `#0A0B10`).
- **Teleprompter Responsivo (`LyricsView.kt`):**
  - Rola automaticamente a letra acompanhando o tempo milimétrico da reprodução musical.
  - Destaca o verso atual com tipografia em evidência, sombra de texto e brilho neon.
  - Suporte a **Seek Interativo**: o usuário pode tocar em qualquer estrofe da letra para avançar ou retroceder a reprodução instantaneamente para aquele ponto da música.
  - Layout adaptativo (`isStageMode`) que preenche dinamicamente o espaço vertical em qualquer proporção de tela (de celulares compactos a tablets), evitando cortes de interface.
- **Contagem Regressiva Animada (Countdown):** Contagem regressiva de 5 segundos antes do início do playback, permitindo ao cantor respirar e se preparar, com opção de pular direto para o show ("Cantar Já").
- **Alternância Instantânea de Stems (Lead Vocal vs Playback Puro):**
  - Botão de alternância em tempo real entre **"Voz Guia"** (com o cantor original auxiliando) e **"Playback Puro"** (somente instrumental), sem perder o sincronismo da posição da faixa.

---

### 3.4. Afinação Vocal em Tempo Real & Motor de Pontuação
- **Detecção de Afinação Vocal (YIN Algorithm):**
  - Captação contínua de áudio via microfone nativo (`AudioRecord`, PCM 16-bit, 44.1kHz mono).
  - Algoritmo YIN de autocorrelação com interpolação parabólica para detecção de frequência fundamental ($F_0$ em Hertz) de 70Hz a 1000Hz (faixa vocal humana completa).
  - Conversão em tempo real da frequência para nota musical padrão (ex: `C4`, `G#4`, `A3`) e cálculo de desvio microtonal em *cents* (-50 a +50 cents).
- **Afinador Visual na Tela:**
  - Exibição contínua da nota cantada, da nota de referência e de um indicador colorido de afinação (verde para afinação precisa, amarelo/vermelho para desvios).
- **Medidor VU sonoro reativo (VU Meter):** Barras equalizadoras animadas na tela alimentadas dinamicamente pela amplitude RMS da voz do usuário.
- **Motor de Pontuação Estilo Arcade (`KaraokeScoringEngine.kt`):**
  - Pontuação clássica acumulativa de **0 a 10.000 pontos**.
  - **Sistema de Combos & Multiplicadores:** Cantar notas afinadas em sequência ativa multiplicadores progressivos de `x1`, `x2`, `x3` e `x4`.
  - **Feedbacks Visuais Instantâneos:** Mensagens flutuantes na tela ("Perfeito!", "Muito Bom!", "Quase lá!", "Tente Novamente!").
  - **Métrica de Precisão Vocal (%):** Avalia a porcentagem exata de quadros em que a voz se manteve no tom correto.
- **Modal de Fim de Show:**
  - Resumo estatístico completo: Pontuação Final, Classificação por Rank (`Rank S`, `Rank A`, `Rank B`, `Rank C`), Classificação por Estrelas (1 a 5 estrelas) e Troféu de Desempenho.

---

### 3.5. Gravação de Performance & Compartilhamento Social
- **Gravação Vocal em Alta Fidelidade:**
  - O KaraoQ grava a performance do usuário durante o show diretamente em formato WAV 16-bit 44.1kHz sem perdas através do `PerformanceRecorder.kt`.
- **Player de Pré-visualização Integrado:**
  - Ao término do show, o usuário pode escutar sua própria gravação antes de decidir compartilhar ou salvar.
- **Compartilhamento Nativo Android (`Intent.ACTION_SEND`):**
  - Exportação direta do arquivo de áudio gravado para qualquer aplicativo instalado no aparelho (WhatsApp, Telegram, Instagram Stories, E-mail, Google Drive) com permissões seguras via `FileProvider`.

---

### 3.6. Biblioteca de Músicas Salvas & Placar Global de Líderes
- **Persistência de Músicas no Storage:**
  - Toda música processada pode ser arquivada no servidor com metadados completos de artista, título, caminhos dos stems e letra sincronizada.
- **Aba "Músicas Salvas":**
  - Lista visual em cartões com artista, título e data de criação.
  - Ação direta de **"Cantar"** que entra imediatamente no palco sem necessidade de reprocessar o áudio.
  - Opção de exclusão individual da biblioteca.
- **Placar Global de Líderes (Leaderboard Cloud):**
  - Top 10 maiores pontuações por música persistidas no backend.
  - Submissão de apelido do cantor ao final da música.
  - Visualização de posições com medalhas de Ouro 🥇, Prata 🥈 e Bronze 🥉.

---

### 3.7. Conectividade & Resiliência de Rede
- **Canais WebSockets Bidirecionais:**
  - `/api/v1/separate/ws/{task_id}`: Transmissão em tempo real da separação Demucs.
  - `/api/v1/lyrics/ws/transcribe/{task_id}`: Transmissão ao vivo de versos detectados pelo Whisper.
- **Fallback Transparente para HTTP Polling:**
  - Se o dispositivo móvel trocar de rede (Wi-Fi para 4G) ou passar por instabilidade onde o WebSocket caia, o aplicativo retoma a verificação via HTTP REST sem travar a interface do usuário.
- **Resolução Dinâmica de URLs (`sanitizeUrl`):**
  - Garante que URLs de áudio funcionem perfeitamente tanto em ambiente local (`http://10.0.2.2:8000` / `localhost` via `adb reverse`) quanto em produção sob HTTPS na VPS (`services-karaoq...`).

---

## 4. Tecnologias Utilizadas

| Camada | Tecnologia | Finalidade |
| :--- | :--- | :--- |
| **Backend Web Framework** | **Python 3.11 + FastAPI** | API REST assíncrona de alta performance e WebSockets |
| **IA de Separação de Áudio** | **Meta Demucs (`htdemucs`)** | Separação de voz e instrumental em 2 stems |
| **IA de Transcrição Local** | **Faster-Whisper (INT8)** | Transcrição vocal com timestamps milimétricos sem custo |
| **IA de Transcrição em Nuvem** | **Google Gemini Audio API** | Fallback multimodal para análise de áudio |
| **Bases de Letras** | **LyricFind API + LRCLIB** | Consulta e sincronização de letras LRC |
| **Codificação de Áudio** | **LAMEenc + FFmpeg** | Geração e conversão de MP3 320kbps |
| **Servidor / Deploy** | **Docker + Uvicorn + Easypanel** | Contêiner Linux otimizado para VPS |
| **App Mobile** | **Kotlin + Jetpack Compose** | Interface moderna, reativa e fluida |
| **Reprodução de Áudio** | **AndroidX Media3 (ExoPlayer)** | Player de áudio com streaming de baixa latência |
| **Captação de Voz** | **Android AudioRecord API** | Leitura de buffers PCM 16-bit a 44.1kHz |
| **Algoritmo de Pitch** | **YIN Pitch Tracking (Nativo)** | Extração de frequência fundamental e nota musical |
| **Networking Mobile** | **Retrofit 2 + OkHttp 4 (WebSockets)** | Comunicação REST e streaming bidirecional |

---

## 5. Jornada do Usuário (User Journey)

1. **Seleção de Faixa:** O usuário abre o KaraoQ e seleciona qualquer música do seu celular. O aplicativo detecta automaticamente o Artista e o Título a partir do nome do arquivo.
2. **Processamento Instantâneo:** Ao tocar em *"Separar Voz e Instrumental"*, o áudio é enviado ao servidor. O usuário acompanha a barra de progresso viva (15% ➔ 30% ➔ 85% ➔ 100%) enquanto a IA isola a voz e o instrumental e busca a letra sincronizada.
3. **Ir para o Palco:** Um botão destacado *"Ir para o Palco 🎤"* surge imediatamente após a separação.
4. **Contagem e Afinação:** Uma contagem regressiva de 5 segundos permite ao cantor posicionar o microfone. O VU Meter e o afinador visual reagem à voz do usuário.
5. **Apresentação:** A letra desliza na tela no ritmo da música. O usuário pode alternar para a voz guia caso precise de ajuda na melodia.
6. **Pontuação e Desempenho:** A cada verso afinado, combos são acumulados e a pontuação sobe.
7. **Resultado & Celebração:** Ao final da música, o placar exibe a pontuação final (ex: 9.450 pts - Rank S), medalha e precisão vocal. O usuário ouve sua gravação, digita seu apelido para o Leaderboard e compartilha seu áudio no WhatsApp ou redes sociais.
8. **Biblioteca Permanente:** A música fica arquivada na aba *"Músicas Salvas"* para ser cantada novamente a qualquer momento sem necessidade de reprocessar.

---

## 6. Diferenciais Competitivos do KaraoQ

- **Liberdade Total de Repertório:** Não depende de catálogos limitados ou assinaturas de faixas pré-montadas. Qualquer música enviada pelo usuário vira karaokê.
- **Tecnologia 100% Autônoma:** Whisper local e Demucs integrados eliminam a obrigatoriedade de APIs pagas de nuvem para funcionamento pleno.
- **Precisão Vocal Real:** Não avalia apenas volume ou presença de som, mas sim a altura tonal exata (pitch em cents) da voz humana.
- **Experiência Visual de Palco:** Interface escura e vibrante projetada especificamente para ambientes de festa, apresentações e diversão entre amigos.
- **Gravação Profissional Inclusa:** Exportação direta da voz isolada em WAV estéreo para compartilhamento imediato.

---

## 7. Roadmap & Futuras Evoluções

- [ ] **Mixagem de Estúdio com Efeitos Vocais em Tempo Real:** Inclusão de DSP no app para aplicação de Reverb de estúdio, Delay dinâmico, Compressor e Equalizador paramétrico na voz do cantor.
- [ ] **Modo Duelo / Batalha de Voz:** Suporte a 2 cantores simultâneos com medição comparativa de afinação lado a lado.
- [ ] **Fila de Karaokê & Modo Festa (Party Mode):** Sistema de fila de espera onde múltiplos usuários na mesma rede ou via QR Code adicionam músicas na vez de cantar.
- [ ] **Transposição de Tom (Pitch Shifting do Playback):** Possibilidade de aumentar ou abaixar semitons do instrumental para adequar ao alcance vocal do cantor.
