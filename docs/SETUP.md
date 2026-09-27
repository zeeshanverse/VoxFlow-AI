# Setup checklist

## A. Local tools

You already have Java 22, Node 24 and npm 11. Maven is handled by `backend/mvnw.cmd`. PostgreSQL's `psql` CLI is optional; pgAdmin is enough to create the database.

## B. PostgreSQL

Create database `voxflow`.

The application uses `spring.jpa.hibernate.ddl-auto=update` for local development. Do not use this as the long-term production migration strategy; add Flyway/Liquibase before a serious public release.

## C. API keys

Backend only:

- AssemblyAI: `ASSEMBLYAI_API_KEY`
- ElevenLabs: `ELEVENLABS_API_KEY`, `ELEVENLABS_VOICE_ID`
- Gemini: `GEMINI_API_KEY`, `GEMINI_MODEL`

Never place these in `frontend/.env` or source code.

## D. Backend

```powershell
cd backend
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

If Windows blocks the wrapper, run:

```powershell
Unblock-File .\mvnw.cmd
.\mvnw.cmd spring-boot:run
```

## E. Frontend

```powershell
cd frontend
copy .env.example .env
npm install
npm run dev
```

## F. Browser permission

Allow microphone access for `http://localhost:5173`.

No camera, location or filesystem permission is required.

## G. First tests

1. Open the frontend.
2. Register an account.
3. Login.
4. Say `What time is it?`.
5. Say a general question; this exercises the LLM.
6. Switch to Java Interview.
7. Start an interview and answer a question.
8. Check the returned score and feedback.