<div align="center">

# 🧠 Nexus AI

### Multi-Modal Personal Knowledge Assistant

[![Java](https://img.shields.io/badge/Java-25-orange?style=for-the-badge&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.36.2-blue?style=for-the-badge)](https://docs.langchain4j.dev/)
[![License](https://img.shields.io/badge/License-MIT-purple?style=for-the-badge)](LICENSE)

**Nexus AI** is a production-grade, multi-modal AI operating system built on **Spring Boot** and **LangChain4j**. It routes conversations across **5 AI models**, supports **live webcam vision**, **voice I/O**, **autonomous web scraping**, **DALL-E image generation**, **semantic vector RAG**, and an **autonomous terminal sandbox** — all from a single glassmorphism interface.

### 🌐 [Live Demo → nexus-ai-ywm3.onrender.com](https://nexus-ai-ywm3.onrender.com)

[Features](#-features) · [Architecture](#-architecture) · [Quick Start](#-quick-start) · [API Reference](#-api-reference) · [Configuration](#%EF%B8%8F-configuration) · [Tech Stack](#-tech-stack)

</div>

---

## ✨ Features

### 🤖 Multi-Model Intelligence
| Model | Provider | Inference | Specialty |
|---|---|---|---|
| **Gemini Flash** | Google | Cloud | Vision + Documents |
| **LLaMA-3.3 70B** | Groq | ⚡ Ultra-Fast (LPU) | General Intelligence |
| **GPT-4 Turbo** | OpenAI | Cloud | Tools + Code |
| **LLaMA-3.1** | Ollama | Local / Offline | Privacy-First |
| **DALL-E 3** | OpenAI | Cloud | Image Generation |

> Hot-swap between models in real-time via the UI dropdown — no restart required.

### 🛠️ Agentic Tool System
The AI has access to **8 autonomous tools** via LangChain4j's `@Tool` framework:

| Tool | Description |
|---|---|
| 🧮 **Math Engine** | Add, subtract, multiply, divide, square root |
| 🕐 **Time Intelligence** | Real-time date, time, and timezone |
| 💻 **System Info** | OS name, version, Java runtime |
| 🌐 **Web Scraper** | Autonomously downloads and reads any URL (Jsoup) |
| 🖥️ **Terminal Sandbox** | Executes PowerShell commands on the host machine |

### 📄 Document Intelligence (Semantic RAG)
- **Apache Tika** parser for PDF, DOCX, TXT, and 1000+ file formats
- **AllMiniLmL6V2** local embedding model (no API calls for embeddings)
- **InMemoryEmbeddingStore** with chunked document splitting (1000 tokens, 200 overlap)
- Automatic semantic similarity retrieval with configurable `minScore` threshold

### 🎨 Image Generation
- Type `/imagine <prompt>` to generate original images via **DALL-E 3**
- Images are returned as inline clickable links in the chat

### 🎯 Frontend Experience
| Feature | Technology |
|---|---|
| 📷 **Live Webcam Vision** | WebRTC + Canvas API |
| 🎤 **Speech-to-Text** | Web Speech API |
| 🔊 **Text-to-Speech** | SpeechSynthesis API |
| 🛑 **Stream Kill-Switch** | AbortController |
| 💡 **Smart Quick Replies** | Context-aware suggestion pills |
| ⚡ **Telemetry** | Millisecond response timing badges |
| 🎭 **AI Personas** | Developer, Novelist, Sarcastic Robot |
| 🌙 **Theme Engine** | Dark/Light mode with localStorage |
| 📥 **Export Chat** | Markdown (.md) download |
| 🖨️ **PDF Export** | Print-optimized CSS engine |
| 📎 **Drag & Drop** | File upload with preview badges |
| 📱 **PWA** | Installable as native desktop app |
| 🧠 **Quiz Arena** | 40-question game (Math, GK, Science, Tech) |

### 🔐 Authentication
- **User Registration** with role-based identity (Developer, Student, Researcher, Admin, Viewer)
- **Session Management** via localStorage with auth guards
- **User Profile** display with avatar, name, and role badge
- **3 Pre-configured Demo Accounts** for instant access

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Browser (Frontend)                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ │
│  │ login.html│ │index.html│ │ quiz.html│ │ PWA    │ │
│  └──────────┘ └──────────┘ └──────────┘ └────────┘ │
│         │            │                               │
│    ┌────▼────────────▼─────────────────────┐        │
│    │         app.js (512 lines)             │        │
│    │  WebRTC │ SSE Stream │ Speech │ Abort  │        │
│    └────────────────┬──────────────────────┘        │
└─────────────────────┼───────────────────────────────┘
                      │ POST /api/nexus/chat (multipart)
                      ▼
┌─────────────────────────────────────────────────────┐
│              Spring Boot 3.2.4 (Port 8080)          │
│  ┌─────────────────────────────────────────────┐    │
│  │         NexusRagController.java              │    │
│  │  ┌─────────┐ ┌─────────┐ ┌───────────────┐ │    │
│  │  │ Gemini  │ │  Groq   │ │ OpenAI / DALLE│ │    │
│  │  └─────────┘ └─────────┘ └───────────────┘ │    │
│  │  ┌─────────┐ ┌─────────────────────────┐   │    │
│  │  │ Ollama  │ │  AgentTools (8 tools)   │   │    │
│  │  └─────────┘ └─────────────────────────┘   │    │
│  └─────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────┐    │
│  │        Semantic RAG Pipeline                 │    │
│  │  Tika Parser → Chunker → MiniLM Embeddings  │    │
│  │  → InMemoryEmbeddingStore → Retriever        │    │
│  └─────────────────────────────────────────────┘    │
│  ┌──────────────┐  ┌───────────────────────┐        │
│  │   H2 (Audit) │  │ ChatMemory (20 msgs)  │        │
│  └──────────────┘  └───────────────────────┘        │
└─────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites
- **Java 21+** (tested on JDK 25)
- **Maven** (included via `mvnw` wrapper)
- API keys for at least one provider (Gemini, OpenAI, or Groq)

### 1. Clone the Repository
```bash
git clone https://github.com/sauravt20250405/Nexus_AI.git
cd Nexus_AI
```

### 2. Configure API Keys
Edit `src/main/resources/application.properties`:
```properties
# Required: At least ONE of these
langchain4j.gemini.chat-model.api-key=YOUR_GEMINI_KEY
langchain4j.open-ai.chat-model.api-key=YOUR_OPENAI_KEY
langchain4j.groq.api-key=YOUR_GROQ_KEY
```

> 💡 **Get free API keys:**
> - Gemini: [ai.google.dev](https://ai.google.dev/)
> - Groq: [console.groq.com](https://console.groq.com/) (Free, ultra-fast)
> - OpenAI: [platform.openai.com](https://platform.openai.com/)

### 3. Run the Application
```bash
# Windows
$env:JAVA_HOME="C:\Program Files\Java\jdk-25"; .\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

### 4. Open in Browser
```
http://localhost:8080
```

### Demo Accounts
| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Administrator |
| `saurav` | `saurav123` | Developer |
| `guest` | `guest123` | Viewer |

---

## 📡 API Reference

### Chat Endpoint
```http
POST /api/nexus/chat
Content-Type: multipart/form-data
```

| Parameter | Type | Description |
|---|---|---|
| `query` | `string` | User message text |
| `model` | `string` | `gemini`, `groq`, `openai`, or `ollama` |
| `files` | `file[]` | Optional file attachments (images, PDFs) |

**Response:** Server-Sent Events (SSE) stream
```json
data: {"type": "token", "content": "Hello"}
data: {"type": "token", "content": " world!"}
```

### Memory Management
```http
DELETE /api/nexus/memory    # Clear AI conversation memory
GET    /api/nexus/history   # Retrieve audit log history
```

### Image Generation
Send a chat message starting with `/imagine`:
```
/imagine A futuristic city skyline at sunset with flying cars
```

---

## ⚙️ Configuration

### `application.properties`

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | Application port |
| `langchain4j.gemini.chat-model.model-name` | `gemini-flash-latest` | Gemini model variant |
| `langchain4j.groq.model-name` | `llama-3.3-70b-versatile` | Groq model variant |
| `langchain4j.open-ai.chat-model.model-name` | `gpt-3.5-turbo` | OpenAI model variant |
| `langchain4j.ollama.chat-model.base-url` | `http://localhost:11434` | Local Ollama endpoint |
| `spring.servlet.multipart.max-file-size` | `50MB` | Max upload file size |
| `spring.mvc.async.request-timeout` | `600000` | SSE stream timeout (ms) |

---

## 🛡️ Security Notes

> ⚠️ **IMPORTANT:** Never commit API keys to version control.

For production deployments:
1. Use environment variables: `export GEMINI_API_KEY=your_key`
2. Reference in properties: `langchain4j.gemini.chat-model.api-key=${GEMINI_API_KEY}`
3. Add `application.properties` to `.gitignore`

---

## 🧰 Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Spring Boot | 3.2.4 | Application framework |
| LangChain4j | 0.36.2 | AI orchestration & tools |
| Apache Tika | 2.9.1 | Document parsing (PDF, DOCX) |
| Jsoup | 1.17.2 | Web scraping |
| AllMiniLmL6V2 | - | Local embedding model |
| H2 Database | - | In-memory audit logging |
| Hibernate/JPA | 6.4 | ORM & data persistence |

### Frontend
| Technology | Purpose |
|---|---|
| Vanilla JS (ES6+) | Application logic |
| CSS3 (Custom) | Glassmorphism design system |
| Web Speech API | Voice input/output |
| WebRTC | Live webcam capture |
| highlight.js | Code syntax highlighting |
| PWA (manifest.json) | Native app installation |

---

## 📁 Project Structure

```
Nexus_Rag/
├── src/main/java/com/nexusrag/
│   ├── NexusRagApplication.java          # Spring Boot entry point
│   ├── agent/
│   │   ├── AssistantAgent.java           # LangChain4j AI interface
│   │   └── tool/
│   │       ├── AgentTools.java           # 8 agentic tools
│   │       └── WebSearchTool.java        # Web search capability
│   ├── config/
│   │   ├── AssistantConfig.java          # 5 model beans + DALL-E
│   │   └── WebConfig.java               # URL routing config
│   ├── controller/
│   │   └── NexusRagController.java       # Main REST controller
│   └── model/                            # JPA entities & repos
├── src/main/resources/
│   ├── application.properties            # App configuration
│   └── static/
│       ├── index.html                    # Main chat interface
│       ├── login.html                    # Authentication page
│       ├── quiz.html                     # Quiz Arena game
│       ├── manifest.json                 # PWA manifest
│       ├── css/style.css                 # Design system (600+ lines)
│       └── js/app.js                     # Frontend engine (530+ lines)
└── pom.xml                               # Maven dependencies
```

---

## 👨‍💻 Author

**Saurav Thakur**

---

<div align="center">

Built with ❤️ using Spring Boot, LangChain4j, and pure determination.

</div>
