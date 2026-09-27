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
DB_URL=<your hosted PostgreSQL JDBC URL>
DB_USERNAME=<database user>
DB_PASSWORD=<database password>
JWT_SECRET=<long random secret>
ASSEMBLYAI_API_KEY=<secret>
ELEVENLABS_API_KEY=<secret>
ELEVENLABS_VOICE_ID=<voice id>
GEMINI_API_KEY=<secret>
GEMINI_MODEL=gemini-3.8-flash
FRONTEND_ORIGIN=https://your-frontend.vercel.app
```

The backend listens on `${PORT:8080}`, so Render can inject its port.

## 3. PostgreSQL in production

Use Neon or another managed PostgreSQL provider.

Create a production database and use its JDBC connection values as Render environment variables.

Do not commit production credentials.

Before public showcase, replace `ddl-auto=update` with versioned migrations.

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

Vercel supports Git-connected React deployments and automatic redeployment from repository changes.

## 5. CORS

Once the Vercel URL exists, set Render:

```text
FRONTEND_ORIGIN=https://YOUR-FRONTEND.vercel.app
```

Redeploy backend.

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