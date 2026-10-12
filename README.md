# VoxFlow AI

A full-stack voice AI assistant and Java interview practice platform. Speak to it, and it transcribes your voice, understands what you want, answers with an LLM, remembers the conversation, and talks back.

**Live demo:** https://vox-flow-ai-sand.vercel.app

> The backend runs on a free hosting tier and sleeps when idle. The first request after a quiet period can take 30–60 seconds while it wakes up.

## Features

- **Voice conversation loop:** record in the browser, transcribe, respond, and play the reply as speech
- **Intent routing:** deterministic commands (start an interview, show history, current time) handled directly; everything else goes to the LLM
- **Persistent conversations:** every message is stored per user and can be reopened later
- **Java interview mode:** question-by-question sessions where you answer by voice, an LLM evaluates each answer, and you get a score and feedback
- **Analytics:** summary of your interview performance
- **JWT authentication** with BCrypt password hashing
- **Rate limiting** per user (Bucket4j) and **cancellable operations** for long-running requests
- **Swappable providers:** speech-to-text, text-to-speech and LLM sit behind interfaces
- **Versioned database migrations** with Flyway

## Architecture

```text
React (Vite)
   │  MediaRecorder -> audio upload
   ▼
Spring Boot REST API  ──►  AssemblyAI   (speech-to-text)
   │                  ──►  Google Gemini (LLM)
   │                  ──►  ElevenLabs    (text-to-speech)
   ▼
PostgreSQL (Neon in production)
```

Request flow for one voice turn:

1. The browser records audio and uploads it.
2. The backend sends it to AssemblyAI and gets a transcript.
3. `IntentRouter` decides between a built-in command and a general LLM query.
4. The reply is saved to the conversation in PostgreSQL.
5. ElevenLabs turns the reply into audio, and React plays it.

Backend layers:

```text
Controllers  ->  Services  ->  Provider interfaces  ->  JPA repositories / external APIs
```

| Provider interface | Implementation |
|---|---|
| `SpeechToTextProvider` | AssemblyAI |
| `TextToSpeechProvider` | ElevenLabs |
| `LlmProvider` | Google Gemini (an OpenAI provider is also included) |

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite 7 |
| Backend | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway |
| Database | PostgreSQL (Neon in production) |
| Auth | JWT (jjwt), BCrypt |
| AI services | AssemblyAI, Google Gemini, ElevenLabs |
| Deployment | Vercel (frontend), Render with Docker (backend), Neon (database) |

## Project structure

```text
VoxFlow-Assistant/
├── backend/            Spring Boot application
│   ├── src/main/java/com/voxflow/
│   │   ├── assistant/      intent routing, rate limiting, cancellation
│   │   ├── auth/           register and login
│   │   ├── conversation/   conversations and messages
│   │   ├── interview/      sessions, questions, evaluation
│   │   ├── llm/            Gemini and OpenAI providers
│   │   ├── voice/          AssemblyAI and ElevenLabs providers
│   │   ├── security/       JWT filter and security config
│   │   ├── config/         datasource and web client config
│   │   └── ...
│   ├── src/main/resources/db/migration/   Flyway migrations
│   └── Dockerfile
├── frontend/           React + Vite app
├── docs/               API, architecture, setup, deployment notes
└── render.yaml         Render service definition
```

## Running locally

### Prerequisites

- JDK 21+
- Node.js 20+
- PostgreSQL 15+
- API keys for AssemblyAI, ElevenLabs and Google Gemini

### 1. Create the database

```sql
CREATE DATABASE voxflow;
```

Flyway creates the tables automatically on first start.

### 2. Configure the backend

Copy `backend/.env.example` to `backend/.env` and fill in your values:

```text
DB_URL=jdbc:postgresql://localhost:5432/voxflow
DB_USERNAME=postgres
DB_PASSWORD=your_password

JWT_SECRET=a_random_secret_of_at_least_32_characters

ASSEMBLYAI_API_KEY=...
ELEVENLABS_API_KEY=...
ELEVENLABS_VOICE_ID=...

GEMINI_API_KEY=...
GEMINI_MODEL=gemini-3.5-flash-lite

FRONTEND_ORIGIN=http://localhost:5173,http://127.0.0.1:5173
```

### 3. Start the backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API runs at `http://localhost:8080`. Check `http://localhost:8080/api/health`.

### 4. Start the frontend

```powershell
cd frontend
copy .env.example .env
npm install
npm run dev
```

Open `http://localhost:5173` and allow microphone access when the browser asks.

### 5. Run the tests

```powershell
cd backend
.\mvnw.cmd clean test
```

## Environment variables

### Backend

| Variable | Purpose |
|---|---|
| `DB_URL` | Database URL. Accepts `jdbc:postgresql://host/db` or a `postgresql://user:password@host/db` string as shown by Neon |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials (not needed if they are inside `DB_URL`) |
| `JWT_SECRET` | Signing key, at least 32 characters |
| `ASSEMBLYAI_API_KEY` | Speech-to-text |
| `ELEVENLABS_API_KEY`, `ELEVENLABS_VOICE_ID` | Text-to-speech |
| `GEMINI_API_KEY`, `GEMINI_MODEL` | LLM |
| `FRONTEND_ORIGIN` | Allowed CORS origin(s), comma-separated, exact match with no trailing slash |

### Frontend

| Variable | Purpose |
|---|---|
| `VITE_API_BASE_URL` | Backend base URL including `/api`, for example `https://your-backend.onrender.com/api` |

`VITE_*` values are embedded in the browser bundle at build time, so never put secrets in them.

## API overview

All endpoints except `auth` and `health` require an `Authorization: Bearer <token>` header.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Create an account |
| POST | `/api/auth/login` | Log in and receive a JWT |
| POST | `/api/voice/transcribe` | Upload audio, get a transcript |
| POST | `/api/voice/speak` | Convert text to speech (`audio/mpeg`) |
| POST | `/api/assistant/message` | Send a message, get the routed intent and reply |
| POST | `/api/operations/cancel` | Cancel an in-progress operation |
| GET | `/api/conversations` | List conversations |
| GET | `/api/conversations/{id}` | Get one conversation with messages |
| POST | `/api/interviews` | Start an interview session |
| POST | `/api/interviews/{id}/answer` | Submit an answer, get score and feedback |
| GET | `/api/interviews/{id}` | Get a session result |
| GET | `/api/analytics` | Interview analytics |
| GET / PUT | `/api/profile` | Read or update the profile |
| GET | `/api/health` | Health check (public) |

More detail is in [`docs/API.md`](docs/API.md).

## Deployment

| Part | Platform | Notes |
|---|---|---|
| Frontend | Vercel | Root directory `frontend`, build `npm run build`, output `dist` |
| Backend | Render | Docker, root directory `backend`, health check `/api/health` |
| Database | Neon | Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` in Render |

Order of setup:

1. Deploy the backend and confirm `/api/health` returns `UP`.
2. Set `VITE_API_BASE_URL` on Vercel and redeploy the frontend.
3. Set `FRONTEND_ORIGIN` on Render to the exact Vercel URL.

Full steps are in [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

## Security notes

- Never commit `backend/.env` or `frontend/.env`. Only the `.env.example` files belong in Git.
- Keep all API keys and database credentials in the hosting provider's environment variables, never in source code or `render.yaml`.
- If a secret ever appears in a log, chat or commit, rotate it immediately.
- Passwords are hashed with BCrypt, and sessions are stateless JWTs.

## Roadmap

- Streaming speech-to-text and text-to-speech
- Tool / function calling
- Retrieval-augmented answers (RAG)
- Refresh-token rotation
- Redis-backed caching and rate limiting
- Structured logging, metrics and tracing
- Async job processing

## Author

Built by [Mohammed Zeeshan](https://zeeshanverse.github.io/Portfolio-Zeeshan/).