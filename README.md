# VoxFlow-Assistant

**Context-Aware Voice AI & Interview Assistant**

VoxFlow-Assistant is a full-stack voice AI application being built with **React** and **Java Spring Boot**. The goal is to create a voice-first assistant that can understand spoken requests, maintain conversation context, execute application-level commands, and provide a dedicated voice-based technical interview experience.

## Architecture

```text
React Frontend
      ↓
Spring Boot Backend
      ↓
Speech-to-Text
      ↓
Intent / Assistant Engine
      ↓
Text-to-Speech
      ↓
React Frontend
```

## Planned Features

* 🎙️ Voice recording and playback
* 🗣️ Speech-to-Text
* 🔊 Text-to-Speech
* 🧠 Context-aware conversations
* 🔀 Intent routing
* ⚙️ Application commands
* 💬 Conversation history and memory
* ☕ Java technical interview mode
* 📊 Interview evaluation and analytics
* 🔐 Authentication and authorization
* 🗄️ PostgreSQL persistence
* 🤖 LLM integration
* 🔌 AI provider abstraction
* 🚀 Production deployment

## Tech Stack

### Frontend

* React
* JavaScript / TypeScript
* MediaRecorder API
* Web Audio API

### Backend

* Java
* Spring Boot
* Spring Web
* Spring WebFlux
* Spring Data JPA
* Spring Security
* JWT
* Maven

### Database

* PostgreSQL

### AI / Voice

* AssemblyAI
* ElevenLabs
* LLM

### Deployment

* Vercel
* Render
* Neon

## Development Status

> 🚧 **Currently in development**

The project is being developed incrementally, with the application architecture and individual components being implemented and tested step by step.

### Current Progress

* [x] Project repository created
* [x] Initial project documentation
* [ ] Spring Boot backend
* [ ] Voice API
* [ ] Speech-to-Text integration
* [ ] Text-to-Speech integration
* [ ] Intent routing
* [ ] AI assistant
* [ ] PostgreSQL persistence
* [ ] Authentication
* [ ] Java interview mode
* [ ] Interview analytics
* [ ] Production deployment

## Project Goals

The project focuses on practical full-stack engineering concepts including:

* REST API design
* Clean architecture
* Dependency injection
* Secure third-party API integration
* Database design
* Authentication
* Error handling
* AI service integration
* Provider abstraction
* Observability and performance monitoring

## Project Structure

```text
VoxFlow-Assistant/
│
├── frontend/     # React application
├── backend/      # Spring Boot application
├── docs/         # Architecture and project documentation
└── README.md
```

## License

This project is currently being developed as a personal portfolio and learning project.
