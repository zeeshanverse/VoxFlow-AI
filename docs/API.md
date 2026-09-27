# API Reference

## Voice

### POST /api/voice/transcribe

Multipart field:

```text
audio=<audio file>
```

Response:

```json
{
  "text": "start a Java interview"
}
```

### POST /api/voice/speak

Request:

```json
{
  "text": "Java interview mode is ready."
}
```

Response:

```text
audio/mpeg
```

## Assistant

### POST /api/assistant/message

Request:

```json
{
  "text": "start a Java interview"
}
```

Response:

```json
{
  "intent": "START_INTERVIEW",
  "response": "Java interview mode is ready. Your first question will focus on core Java."
}
```
