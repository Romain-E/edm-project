# Document Management REST API

A production-ready backend service for async document archival, built with **Java 17 + Spring Boot 3**.  
Documents are stored in **Amazon S3** (or LocalStack locally), events are streamed via **Apache Kafka**, and all endpoints are secured with **JWT authentication**.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (jjwt) |
| Storage | Amazon S3 / LocalStack |
| Messaging | Apache Kafka |
| Database | PostgreSQL + Spring Data JPA |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Monitoring | Spring Actuator |
| Containerization | Docker + Docker Compose |
| Testing | JUnit 5, Mockito |

---

## Getting Started

### Prerequisites

- Java 17+
- Docker & Docker Compose

### Run locally (one command)

```bash
docker-compose up --build
```

This starts:
- The Spring Boot app on `http://localhost:8080`
- PostgreSQL on port `5432`
- Kafka + Zookeeper on port `9092`
- LocalStack (S3 emulator) on port `4566`
- Kafdrop (Kafka Web UI) on port `9000`
- Auto-creates the S3 bucket `docmanager-bucket`

---

## API Reference

### Authentication

#### Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john",
  "email": "john@example.com",
  "password": "secret123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "john",
  "role": "ROLE_USER"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "john",
  "password": "secret123"
}
```

---

### Documents

All document endpoints require the JWT token in the `Authorization` header:
```
Authorization: Bearer <your_token>
```

#### Upload a document
```http
POST /api/documents
Content-Type: multipart/form-data

file: <your_file>
```

Response `201 Created`:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "filename": "uuid_invoice.pdf",
  "originalFilename": "invoice.pdf",
  "contentType": "application/pdf",
  "fileSize": 45231,
  "uploadedBy": "john",
  "status": "PENDING",
  "createdAt": "2024-01-15T10:30:00"
}
```

#### List documents (paginated)
```http
GET /api/documents?page=0&size=20
```

Response:
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

#### Get document + download URL
```http
GET /api/documents/{id}
```

Returns a **pre-signed S3 URL** (valid 15 minutes) for direct download without exposing credentials.

#### Delete document
```http
DELETE /api/documents/{id}
```

Response `204 No Content`

---

## Kafka Events

Every upload and delete publishes an event to Kafka:

| Topic | Event | Trigger |
|---|---|---|
| `document.uploaded` | `DocumentUploadedEvent` | File uploaded successfully |
| `document.deleted` | `DocumentDeletedEvent` | File deleted |

Event payload example:
```json
{
  "documentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "filename": "invoice.pdf",
  "s3Key": "documents/john/uuid_invoice.pdf",
  "uploadedBy": "john",
  "fileSize": 45231,
  "uploadedAt": "2024-01-15T10:30:00"
}
```

---

## API Documentation

Swagger UI available at: `http://localhost:8080/swagger-ui.html`

---

## Health Check

```http
GET /actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "kafka": { "status": "UP" }
  }
}
```

---

## Architecture

```
Client
  │
  ▼
DocumentController  ──►  DocumentService  ──►  S3Service (Amazon S3)
                               │
                               ├──►  DocumentRepository (PostgreSQL)
                               │
                               └──►  KafkaProducerService ──► Kafka Topics
                                         document.uploaded
                                         document.deleted
```

---

## Running Tests

```bash
mvn test
```

Tests use H2 in-memory database and embedded Kafka — no external dependencies needed.

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `docmanager` | PostgreSQL username |
| `DB_PASSWORD` | `docmanager` | PostgreSQL password |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `S3_BUCKET_NAME` | `docmanager-bucket` | S3 bucket name |
| `S3_ENDPOINT` | *(empty)* | Override for LocalStack |
| `AWS_REGION` | `us-east-1` | AWS region |
| `JWT_SECRET` | *(change in prod)* | JWT signing key (min 256 bits) |

---

## Author

**Romain Emery** — Senior Java/Spring Boot Backend Engineer  
[LinkedIn](https://www.linkedin.com/in/romain-emery-219981174/) · [GitHub](https://github.com/Romain-E)
