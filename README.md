# SmartDoc API

> AI-powered document intelligence platform — RAG-based Q&A, summarization, and structured data extraction.

![CI](https://github.com/ShR919/smartdoc-api/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0-blue)

---

## What It Does

Upload any document (PDF, DOCX, TXT) and SmartDoc API will:

- **Index it** — extract text, chunk it, generate embeddings, store in pgvector
- **Answer questions** — RAG pipeline retrieves relevant chunks and grounds LLM responses
- **Summarize** — returns a structured summary with key bullet points
- **Stream responses** — real-time token streaming via Server-Sent Events

---

## Architecture

```
Client
  │
  ▼
DocumentController  ──► DocumentService ──► TextExtractionService (Apache Tika)
                                       └──► VectorStore (pgvector) ◄── Spring AI Embeddings
  │
  ▼
QueryController  ──► QueryService ──► VectorStore (similarity search)
                                 └──► ChatClient (OpenAI GPT-4o)
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2, Spring AI 1.0 |
| LLM | OpenAI GPT-4o + text-embedding-3-small |
| Vector DB | PostgreSQL + pgvector |
| Document Parsing | Apache Tika |
| Database | PostgreSQL 16 |
| Containerisation | Docker, docker-compose |
| CI/CD | GitHub Actions |
| API Docs | Swagger / OpenAPI |

---

## Getting Started

### Prerequisites
- Java 21
- Docker + docker-compose
- OpenAI API key

### 1. Clone and configure

```bash
git clone https://github.com/ShR919/smartdoc-api.git
cd smartdoc-api
cp .env.example .env
# Add your OPENAI_API_KEY to .env
```

### 2. Start the database

```bash
docker-compose up postgres -d
```

### 3. Run the API

```bash
./mvnw spring-boot:run
```

API available at `http://localhost:8080`
Swagger UI at `http://localhost:8080/swagger-ui.html`

---

## API Reference

### Upload a document
```bash
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -F "file=@contract.pdf"
```

### Ask a question (RAG)
```bash
curl -X POST http://localhost:8080/api/v1/documents/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the notice period for termination?"}'
```

### Stream a response
```bash
curl -X POST http://localhost:8080/api/v1/documents/query/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"question": "Summarize the key obligations"}'
```

### Summarize a document
```bash
curl http://localhost:8080/api/v1/documents/{id}/summary
```

### List all documents
```bash
curl http://localhost:8080/api/v1/documents
```

---

## Project Structure

```
src/main/java/com/smartdoc/
├── controller/      # REST endpoints
├── service/         # Business logic — RAG pipeline, document processing
├── model/           # JPA entities
├── dto/             # Request/response objects
├── repository/      # Spring Data JPA repos
├── config/          # Spring AI ChatClient, OpenAPI config
└── exception/       # Global error handling
```

---

## Roadmap

- [x] Document upload + async indexing
- [x] RAG-based Q&A with source citations
- [x] Streaming responses (SSE)
- [x] Document summarization
- [x] Multi-document query filtering
- [ ] Chat sessions with conversation history
- [ ] Structured data extraction (JSON output)
- [ ] API key authentication
- [ ] Rate limiting

---

## Author

**Shruti Raj** — [linkedin.com/in/shrutiraj7](https://linkedin.com/in/shrutiraj7) | [github.com/ShR919](https://github.com/ShR919)
