# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
./gradlew run

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.salles.ServerTest"

# Build the project
./gradlew build

# Build a fat JAR
./gradlew buildFatJar
```

## Project Structure



## Architecture

This is a Ktor server application (Kotlin, JVM 21) using Netty as the engine. The entry point is `EngineMain` — modules are wired in `src/main/resources/application.yaml`, not programmatically in `main.kt`.

**Module registration order** (in `application.yaml`):
1. `configureSerialization` — installs `ContentNegotiation` with `kotlinx.serialization` JSON
2. `configurePostgres` — connects to the database and registers CRUD routes under `/cities`
3. `configureKoin` — sets up Koin DI; currently injects a `HelloService` singleton
4. `configureRouting` — registers the root `/` and `/json/kotlinx-serialization` routes

**Database**: `connectToPostgres(embedded = Boolean)` in `Postgres.kt` switches between an in-process H2 database (used by default/tests, `embedded = true`) and a real PostgreSQL connection configured via `postgres.url`, `postgres.user`, and `postgres.password` in the YAML config. The `CityService` uses raw JDBC (no ORM).

**Dependency injection**: Koin modules are defined inline inside `configureKoin()`. New services should be registered there.

**Testing**: Tests use `ktor-server-test-host`. The `testApplication { configure() }` block loads the default `application.yaml` configuration.
