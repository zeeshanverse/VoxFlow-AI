# Deployment plan

The project is designed as two deployable applications:

```text
GitHub temporary/private repository
        ├── frontend -> Vercel
        └── backend  -> Render
                         |
                         └── PostgreSQL/Neon
```

## 1. Temporary GitHub repository

From the project root:

```powershell
git init
git branch -M main
git add .
git commit -m "Initial VoxFlow full-stack implementation"
git remote add origin https://github.com/YOUR_USERNAME/VoxFlow-Assistant-Temp.git
git push -u origin main
```

Before pushing, verify:

```powershell
git status
git ls-files | findstr ".env"
```

The second command should NOT show real `.env` files — only `backend/.env.example` and `frontend/.env.example` (if present) are expected there.

## 2. Backend on Render

Render supports Git-connected web services and Docker deployments. For this project use the included backend Dockerfile.

Create a Render Web Service:

- Repository: temporary GitHub repo
- Root Directory: `backend`
- Runtime: Docker
- Dockerfile Path: `backend/Dockerfile` if Render is configured from repo root; otherwise `Dockerfile`
- Health check: `/api/health`

If Render detects `render.yaml` at the repo root, it can create this service automatically as a Blueprint. Either way, the variable **names** are defined in `render.yaml` — the real **values** are entered manually in the Render dashboard, never written into `render.yaml` itself, since that file is committed to Git.

Environment variables to enter in the Render dashboard:

```text
DB_URL=jdbc:postgresql://<neon-host>/<db-name>?sslmode=require
DB_USERNAME=<database user>
DB_PASSWORD=<database password>
JWT_SECRET=<long random secret>
ASSEMBLYAI_API_KEY=<secret>
ELEVENLABS_API_KEY=<secret>
ELEVENLABS_VOICE_ID=<voice id>
GEMINI_API_KEY=<secret>
GEMINI_MODEL=gemini-3.5-flash-lite
FRONTEND_ORIGIN=https://your-frontend.vercel.app
```

`DB_URL` is accepted in either format: the clean JDBC form above (user and password in `DB_USERNAME` / `DB_PASSWORD`), or the `postgresql://user:password@host/db` string exactly as Neon displays it. Never put real values in `application.properties`; it only holds local defaults.

The backend listens on `${PORT:8080}`, so Render can inject its port.

## 3. PostgreSQL in production

Use Neon or another managed PostgreSQL provider.

Create a production database. In the Neon console, copy the host and database name, then enter them in Render as described above. Neon's connection string starts with `postgresql://`, not `jdbc:postgresql://`.

The schema is managed by Flyway migrations (`backend/src/main/resources/db/migration`) and Hibernate runs with `ddl-auto=validate`. Any schema change needs a new migration file (`V2__...sql`); never edit `V1`.

Do not commit production credentials. If a password ever appears in a log, a chat, or Git, rotate it in Neon immediately.

## 4. Frontend on Vercel

Import the same GitHub repository.

Set the Vercel Root Directory to:

```text
frontend
```

Build command:

```text
npm run build
```

Output directory:

```text
dist
```

Environment variable (set in the Vercel dashboard, not in `frontend/.env`):

```text
VITE_API_BASE_URL=https://YOUR-RENDER-BACKEND.onrender.com/api
```

Vite reads `VITE_*` variables at build time. After changing this value, trigger a new deployment in Vercel; a restart is not enough.

Vercel supports Git-connected React deployments and automatic redeployment from repository changes.

## 5. CORS

Once the Vercel URL exists, set Render:

```text
FRONTEND_ORIGIN=https://YOUR-FRONTEND.vercel.app
```

Use the exact origin, with no trailing slash. Multiple origins can be separated by commas. Redeploy the backend after changing it.

## 6. Deployment test

Test:

```text
GET https://YOUR-RENDER-BACKEND.onrender.com/api/health
```

Then open the Vercel site and test:

- register
- login
- microphone
- STT
- assistant/LLM
- TTS
- conversation persistence
- interview
- analytics

## 7. Future public showcase

When you are ready:

1. Stop changing deployment secrets.
2. Copy the project root to your intended showcase repository.
3. Initialize/replace the Git remote.
4. Push to the showcase repository.
5. Point Render/Vercel to the showcase repository.
6. Re-enter environment variables in the deployment dashboards.
7. Never copy `.env` files.