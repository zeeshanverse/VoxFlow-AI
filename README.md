# VoxFlow AI

A full-stack voice AI assistant and Java interview platform.

## What it does

VoxFlow connects a browser microphone to a Java/Spring Boot backend:

```text
React
  -> MediaRecorder
  -> Spring Boot
  -> AssemblyAI STT
  -> Assistant / Intent / LLM
  -> PostgreSQL memory
  -> ElevenLabs TTS
  -> React audio playback
```

It also includes:

- JWT authentication
- user profiles
- persistent conversations and messages
- LLM-backed assistant responses
- deterministic command routing
- Java interview sessions
- spoken answer transcription
- LLM-based answer evaluation
- interview scoring and analytics
- provider abstractions for STT/TTS/LLM
- health endpoint
- Docker deployment files
- Render and Vercel deployment guidance

## Stack

Frontend: React + Vite  
Backend: Java 21+ + Spring Boot + Spring Security + JPA  
Database: PostgreSQL  
STT: AssemblyAI  
TTS: ElevenLabs  
LLM: Google Gemini API (generateContent)  
Deployment: Vercel + Render + PostgreSQL/Neon

## Local development

### 1. Prerequisites

- JDK 21+ (your JDK 22 is supported)
- Node.js 20+
- PostgreSQL 15+
- Git
- Internet access for Maven/npm and external AI APIs

Maven is supplied through `mvnw.cmd`, so a global Maven installation is not required. On Windows, the wrapper also loads `backend/.env` for local `spring-boot:run`.

### 2. Database

Create:

```sql
CREATE DATABASE voxflow;
```

### 3. Backend configuration

Copy:

```text
backend/.env.example -> backend/.env
```

Fill in real values inside `backend/.env` only — never edit `.env.example` with real secrets:

```text
DB_URL=jdbc:postgresql://localhost:5432/voxflow
DB_USERNAME=postgres
DB_PASSWORD=your_password

JWT_SECRET=use_a_long_random_secret
ASSEMBLYAI_API_KEY=...
ELEVENLABS_API_KEY=...
ELEVENLABS_VOICE_ID=...

GEMINI_API_KEY=...
GEMINI_MODEL=gemini-3.8-flash
```

The backend reads environment variables through Spring placeholders.

### 4. Start backend (Windows)

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Backend:

```text
http://localhost:8080
```

Health:

```text
http://localhost:8080/api/health
```

### 5. Start frontend

In another terminal:

```powershell
cd frontend
copy .env.example .env
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

Allow microphone access when the browser asks.

## Core loop

1. Register/login.
2. Press the microphone.
3. Speak.
4. Browser records audio.
5. Spring Boot uploads it to AssemblyAI.
6. Transcript is saved.
7. Assistant routes the request and/or asks the LLM.
8. Conversation is persisted.
9. ElevenLabs generates audio.
10. React plays the response.

## Main endpoints

```text
POST /api/auth/register
POST /api/auth/login

POST /api/voice/transcribe
POST /api/voice/speak

POST /api/assistant/message
GET  /api/conversations
GET  /api/conversations/{id}

POST /api/interviews
POST /api/interviews/{id}/answer
GET  /api/interviews/{id}
GET  /api/analytics

GET  /api/profile
PUT  /api/profile
GET  /api/health
```

## Security

Never commit:

```text
backend/.env
frontend/.env
```

`backend/.env.example` and `frontend/.env.example` are safe to commit — they contain no real values, only variable names, and exist so anyone setting up the project knows what to configure. Never put real secrets into an `.env.example` file.

Never put AssemblyAI, ElevenLabs, Gemini, JWT, or database secrets in React source.

For deployment, put secrets in the hosting provider's environment variables — never inside `render.yaml` itself, even though that file lists the variable names.

## Deployment

See `docs/DEPLOYMENT.md`.

## Status

This repository is intended as a private development project until the implementation is polished for public showcase.