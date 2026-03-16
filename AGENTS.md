# AGENTS.md

## Project Overview

**GitHub Workflow Orchestrator** is a service that orchestrates GitHub Actions workflows triggered by pull request events. It receives GitHub webhook events (`pull_request`, `check_run`), matches them against configurable workflow definitions stored in PostgreSQL, and dispatches GitHub Actions workflows via the GitHub API.

Key capabilities:
- Configurable event filtering (JsonPath matchers, file path matchers, Dependabot matchers)
- Concurrency control via a slot system to limit concurrent workflow runs
- GitHub check run management (creates/updates check runs on PRs)
- Comment callback system (OIDC-authenticated callbacks from dispatched workflows)
- React frontend dashboard for managing workflow definitions (CRUD)

## Tech Stack

| Layer    | Technology                                                                 |
|----------|----------------------------------------------------------------------------|
| Backend  | Kotlin 2.x, Spring Boot 3.x, JDK 21                                      |
| Frontend | React 19, TypeScript 5.8, Vite 6, Mantine UI 7, TanStack Router + Query  |
| Database | PostgreSQL 16, Flyway migrations, Spring Data JDBC (`NamedParameterJdbcTemplate`) |
| Scheduling | db-scheduler (persistent task queue in PostgreSQL)                       |
| Build    | Gradle 9.x (Kotlin DSL), version catalog (`libs.versions.toml`)          |
| Testing  | Kotest (FunSpec), MockK, WireMock, Spring Boot Test (backend); Vitest (frontend) |
| Auth     | Auth0 java-jwt + jwks-rsa for GitHub App JWT authentication               |

## Project Structure

```
.
├── backend/                  # Kotlin/Spring Boot backend
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── kotlin/pl/allegro/tech/github/botorchestrator/
│       │   │   ├── api/          # REST endpoints (webhooks, callbacks)
│       │   │   ├── application/  # Event handlers (orchestration logic)
│       │   │   ├── config/       # Spring configuration
│       │   │   ├── domain/       # Core business logic & interfaces
│       │   │   │   ├── filtering/    # PR matchers (JsonPath, FilePath, Dependabot)
│       │   │   │   ├── workflows/    # Workflow definitions (CRUD, repository, config)
│       │   │   │   ├── comment/      # Comment domain
│       │   │   │   └── dependabot/   # Dependabot version parsing
│       │   │   ├── infra/        # Infrastructure implementations
│       │   │   │   ├── github/       # GitHub API client, auth (JWT, OIDC)
│       │   │   │   ├── postgres/     # JDBC repositories, slots
│       │   │   │   └── task/         # db-scheduler task definitions
│       │   │   └── AppRunner.kt  # Application entry point
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/ # Flyway SQL migrations (V001-V006)
│       ├── test/                 # Unit tests
│       └── integration/          # Integration tests (WireMock, Spring Boot Test)
├── frontend/                 # React/TypeScript frontend
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── main.tsx              # Entry point
│       ├── App.tsx               # Router + React Query setup
│       ├── api.ts                # Axios client with React Query
│       ├── api-types.ts          # Auto-generated from OpenAPI
│       ├── routes/               # File-based routing (TanStack Router)
│       └── components/           # UI components (WorkflowsTable, WorkflowForm, etc.)
├── build-logic/              # Gradle convention plugins
├── docs/                     # Additional documentation
├── compose.yml               # Docker Compose (PostgreSQL for local dev)
├── build.gradle.kts          # Root build script
├── settings.gradle.kts       # Multi-module settings
└── gradle/libs.versions.toml # Dependency version catalog
```

## Architecture

The backend follows a **layered architecture**: `api` -> `application` -> `domain` -> `infra`.

**Request flow:** GitHub Webhook -> REST Endpoint (`api`) -> db-scheduler task (`infra/task`) -> Event Handler (`application`) -> Workflow Dispatcher (`domain`) -> GitHub API Client (`infra/github`)

Key patterns:
- **Domain interfaces** (`GithubClient`, `AvailableSlots`, `WorkflowDefinitionRepository`) with infrastructure implementations (`RestGithubClient`, `PostgresAvailableSlots`, `JdbcWorkflowDefinitionRepository`)
- **Strategy pattern** for PR filtering: `CompoundPullRequestMatcher` with `ANY`/`ALL` matching strategies and pluggable matchers
- **Reliable processing**: Events are scheduled as db-scheduler one-time tasks with retry, not processed synchronously
- **GitHub App auth**: JWT signing with token caching and TTL-based refresh

## Build & Run Commands

### Backend

```bash
./gradlew run              # Run the application (loads .env via build-logic plugin)
./gradlew check            # All tests (unit + integration)
./gradlew test             # Unit tests only
./gradlew integrationTest  # Integration tests only
```

### Frontend

```bash
npm run start              # Dev server on port 3000 (proxies to backend)
npm run build              # TypeScript check + Vite build
npm run test               # Vitest
npm run lint               # ESLint
npm run generate-types     # Regenerate API types from OpenAPI spec
```

### Infrastructure

```bash
docker compose up          # Start PostgreSQL 16 for local development
```

## Code Conventions

- **Kotlin style**: `intellij_idea` ktlint code style, 4-space indent, 160-char line width (see `.editorconfig`)
- **No trailing commas** in Kotlin
- **No star imports** in Kotlin (`name_count_to_use_star_import = 2147483647`)
- **Test style**: Kotest `FunSpec` with `shouldBe` matchers; specs named `*Spec.kt` (unit) and `*IntSpec.kt` (integration)
- **Integration tests**: Located in `backend/src/integration/`, extend `BaseIntegrationSpec`, use WireMock for GitHub API mocking
- **Frontend**: ESLint + Prettier enforced via Husky pre-commit hooks (lint-staged)
- **API types**: Auto-generated from OpenAPI spec via `swagger-typescript-api` -- do not edit `api-types.ts` manually
- **Frontend routing**: File-based with TanStack Router (routes in `frontend/src/routes/`)
- **i18n**: Translations in `frontend/public/locales/{en-US,pl}/translation.json`

## Key Domain Concepts

- **Workflow Definition**: A configuration that maps GitHub events to a specific GitHub Actions workflow to dispatch. Stored in PostgreSQL, managed via REST API + frontend.
- **Slot**: Concurrency control unit. Each dispatched workflow occupies a slot; `max-concurrent-runs` limits how many can run simultaneously.
- **Concurrency Group**: Controls how GitHub Actions handles concurrent runs of the same workflow for the same PR (see `docs/concurrency-group.md`).
- **Matchers/Filters**: Rules that determine whether an incoming PR event should trigger a workflow (JsonPath, file path regex, Dependabot).
- **Check Callback**: URL passed to dispatched workflows so they can report status back to the PR check.
- **Comment Callback**: URL passed to dispatched workflows so they can post comments on the PR (authenticated via GitHub OIDC).

## Environment Variables

See `.env.example` for required configuration:
- `DB_PORT` -- PostgreSQL port
- `APP_BASE_URL` -- Base URL for callback URLs
- GitHub App credentials (app ID, private key, installation ID)

## Important Notes

- The project uses **Gradle composite builds** (`build-logic/` for convention plugins)
- Database migrations are in `backend/src/main/resources/db/migration/` (Flyway, `V001` through `V006`)
- Local seed data: `backend/src/main/resources/db/local/V0099__local.sql`
- Docker Compose is configured for Spring Boot's Docker Compose support (`spring-boot-docker-compose` dependency)
- The frontend build is integrated into Gradle via the `node-gradle` plugin
