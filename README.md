# IntelliCare Backend — Admin & System Modules

AI-powered healthcare platform backend built with Spring Boot 3, JWT security, PostgreSQL, and LangChain4j.

---

## Quick Start (Docker — recommended)

```bash
# 1. Clone and enter project
git clone <repo-url>
cd intellicare-backend

# 2. Copy environment template
cp .env.example .env.prod

# 3. Start the full stack (Postgres + MailHog + Backend)
docker compose up -d

# 4. Swagger UI
open http://localhost:8080/api/swagger-ui.html

# 5. H2 Console (dev profile only)
open http://localhost:8080/api/h2-console
```

---

## Local Development (H2 — no Docker)

```bash
# Requires: Java 21, Maven 3.9+

mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Default admin credentials (change immediately):
# Email:    admin@intellicare.com
# Password: Admin@12345
```

---

## Project Structure

```
src/main/java/com/intellicare/
├── config/          # Security, JWT, AI, OpenAPI, Async
├── controller/      # AuthController, UserController, NotificationController,
│                    # AuditLogController, AIController
├── dto/
│   ├── request/     # AuthRequest, UserRequest, NotificationRequest, AIRequest
│   └── response/    # ApiResponse, AuthResponse, UserResponse, NotificationResponse,
│                    # AuditLogResponse, AIResponse
├── entity/          # User, Role, RefreshToken, Notification, NotificationTemplate,
│                    # AuditLog, AIRequestLog, AIConversation, AIMessage
├── exception/       # GlobalExceptionHandler + domain exceptions
├── repository/      # Spring Data JPA repositories
├── security/        # JwtUtil, JwtAuthenticationFilter, UserDetailsServiceImpl
├── service/         # Interfaces + impl/ (AuthService, UserService, NotificationService,
│   └── impl/        # AuditLogService, AIGatewayService, ConversationService,
│                    # EmailService, SmsService, WhatsAppService, PushNotificationService)
└── util/            # UserMapper, RequestUtil
src/main/resources/
├── application.properties
├── application-dev.properties   (H2)
├── application-prod.properties  (PostgreSQL)
└── db/migration/
    ├── V1__initial_schema.sql
    └── V2__seed_roles_and_admin.sql
```

---

## Modules

| Module | Description |
|---|---|
| **User & Role Management** | Registration, login, JWT/refresh tokens, BCrypt, RBAC (ADMIN/PATIENT/DOCTOR/HOSPITAL), activate/deactivate |
| **Notifications** | Email (JavaMail), SMS (Twilio mock), WhatsApp (Cloud API mock), In-App. Template engine, bulk send, history, read tracking |
| **Audit Logging** | Async audit trail for all critical actions. Searchable by user, action, date, status |
| **AI Orchestration** | LangChain4j + GPT-4o chat, Whisper STT, ElevenLabs TTS. Mock adapters enabled by default. Conversation tracking + token logging |

---

## Integrating into a Larger Project

This module is designed to merge into an existing Spring Boot project:

1. Copy `src/main/java/com/intellicare/**` into your project's source tree
2. Merge `pom.xml` dependencies into your project's `pom.xml`
3. Merge `application*.properties` settings
4. Copy Flyway migrations to `src/main/resources/db/migration/`
5. Adjust `SecurityConfig` to match your existing URL patterns
6. The `@SpringBootApplication` scan will auto-detect all beans

---

## Environment Variables (production)

See `.env.example` for the full list. Key variables:

| Variable | Description |
|---|---|
| `DB_HOST`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `JWT_SECRET` | Must be >= 64 chars. Generate: `openssl rand -base64 64` |
| `OPENAI_API_KEY` | GPT-4o / Whisper key |
| `ELEVENLABS_API_KEY` | TTS key |
| `TWILIO_ACCOUNT_SID` + `TWILIO_AUTH_TOKEN` | SMS |
| `AI_MOCK_ENABLED=false` | Disable mocks in production |

---

## Running Tests

```bash
mvn test
```

Tests use H2 in-memory database — no external services required.

---

## CI/CD

GitHub Actions workflow at `.github/workflows/ci-cd.yml`:

- **PR / push to develop**: run tests
- **Push to main**: test → Docker build → push to GHCR → deploy to staging via SSH

Required GitHub Secrets: `STAGING_HOST`, `STAGING_USER`, `STAGING_SSH_KEY`
