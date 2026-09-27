# Architecture

## Backend

```text
HTTP Controllers
      ↓
Application Services
      ↓
Domain / Provider interfaces
      ↓
JPA repositories + external APIs
```

Provider interfaces keep the application replaceable:

```text
SpeechToTextProvider
  └── AssemblyAI

TextToSpeechProvider
  └── ElevenLabs

LlmProvider
  └── Google Gemini API (generateContent)
```

## Assistant

```text
Transcript
   ↓
IntentRouter
   ├── deterministic command
   └── general query
          ↓
       LLM
   ↓
Conversation persistence
   ↓
TTS
```

## Interview

```text
Interview session
   ↓
Question bank
   ↓
Spoken answer
   ↓
AssemblyAI
   ↓
LLM evaluator
   ↓
score + feedback
   ↓
next question
   ↓
final analytics
```

## Future upgrades

- streaming STT/TTS
- tool/function calling
- RAG
- Redis/session caching
- Flyway migrations
- refresh-token rotation
- rate limiting
- structured logging
- metrics/tracing
- async job processing