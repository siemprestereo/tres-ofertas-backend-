# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application locally
./mvnw spring-boot:run

# Build (compile + package)
./mvnw clean package

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClassName

# Compile only
./mvnw clean compile
```

## Architecture

**Spring Boot REST API** — a professional rating platform (waiters/hospitality staff). Clients rate professionals; professionals build CVs and reputation scores.

### Layered structure
```
Controller → Service/impl → Repository → MySQL (via Flyway migrations)
```

- `controller/` — 14 REST endpoint groups (auth, users, professionals, ratings, cv, qr, etc.)
- `service/impl/` — all business logic lives here
- `model/` — 20 JPA entities (key ones: `AppUser`, `Rating`, `WorkHistory`, `Cv`, `QrToken`)
- `repository/` — Spring Data JPA interfaces
- `config/` — Security, CORS, Cloudinary, Bucket4j rate limiting
- `security/` — JWT filter and OAuth2 handlers
- `dto/` — request/response objects separate from entities
- `db/migration/` — 34 Flyway SQL migrations (V1–V34), schema is `validate` mode (never auto-created by Hibernate)

### Authentication
- Stateless JWT (secret from `JWT_SECRET` env var, 15-day expiry)
- Google OAuth2 with two separate registrations: one for the CLIENT role, one for the PROFESSIONAL role
- Three roles: `ADMIN`, `CLIENT`, `PROFESSIONAL`

### Key external integrations
- **Cloudinary** — image/file uploads
- **ZXing** — QR code generation (professionals share QR codes for clients to submit ratings)
- **iText 7** — PDF generation (CV export)
- **Bucket4j** — rate limiting
- **Zoho SMTP** — transactional email

### Configuration profiles
- `application.properties` — local development defaults
- `application-prod.properties` — Railway deployment overrides (reads `DATABASE_URL`, `PORT`, etc.)

### Required environment variables
`DATABASE_URL`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET`, `ZOHO_APP_PASSWORD`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `FRONTEND_URL`, `BACKEND_URL`

### Database migrations
All schema changes go in `src/main/resources/db/migration/` as `V{n}__{description}.sql`. Flyway runs them automatically on startup. Never use Hibernate DDL to modify schema.
