import hmac
import os
import tempfile
import wave

from fastapi import FastAPI, Header, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel, Field
from piper import PiperVoice

VOICE_PATH = os.getenv("PIPER_VOICE_PATH", "/app/en_US-lessac-medium.onnx")
TOKEN = os.getenv("PIPER_TTS_TOKEN", "")

if not TOKEN:
    raise RuntimeError("PIPER_TTS_TOKEN is required")

voice = PiperVoice.load(VOICE_PATH)
app = FastAPI(title="VoxFlow Piper TTS")


class SynthesizeRequest(BaseModel):
    text: str = Field(min_length=1, max_length=5000)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/synthesize")
def synthesize(
    request: SynthesizeRequest,
    x_piper_token: str | None = Header(default=None),
):
    if not x_piper_token or not hmac.compare_digest(x_piper_token, TOKEN):
        raise HTTPException(status_code=401, detail="Unauthorized")

    temp_path = None
    try:
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as temp:
            temp_path = temp.name

        with wave.open(temp_path, "wb") as wav_file:
            voice.synthesize_wav(request.text, wav_file)

        with open(temp_path, "rb") as audio_file:
            audio = audio_file.read()

        return Response(content=audio, media_type="audio/wav")
    finally:
        if temp_path:
            try:
                os.remove(temp_path)
            except OSError:
                pass
