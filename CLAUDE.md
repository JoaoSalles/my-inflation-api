# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
./gradlew run

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.salles.scrapping.PAScrapperTest"

# Build the project
./gradlew build

# Build a fat JAR
./gradlew buildFatJar
```

## Project Structure

```
src/main/kotlin/com/salles/
├── Application.kt                        # Ktor module: wires Koin DI, JSON serialization, scrapping routes
└── scrapping/
    ├── data/                             # DTOs / API layer
    │   ├── PASearchRequest.kt            # Request body for Pão de Açúcar search API
    │   ├── PASearchResponse.kt           # Response DTO; normalizes names, deserializes centavo prices
    │   └── ProductToScrap.kt             # Concrete impl of domain ProductToScrap
    ├── domain/                           # Interfaces and value types
    │   ├── ProductToScrap.kt             # Interface: name, keywords, quantity base
    │   ├── QuantityBase.kt               # Enum: GRAMS, UNITS, MILLILITERS
    │   ├── ScrapperInterface.kt          # Generic Scrapper<T> interface
    │   └── SearchResponse.kt             # Interface: price (Int?), name (String)
    ├── routes/
    │   └── ScrappingRoutes.kt            # GET /scrapping — triggers sugar scraping
    ├── scrapers/
    │   └── PAScrapper.kt                 # Pão de Açúcar scraper; filters by keyword/brand, computes per-unit price
    ├── services/
    │   └── ScrappingService.kt           # Launches scrapers with a SupervisorJob coroutine scope
    └── utils/
        └── MillicentUtils.kt             # normalizeForMillicent / denormalizeFromMillicent
```

## Architecture

This is a Ktor server application (Kotlin, JVM 21) using Netty as the engine. The single module is registered in `src/main/resources/application.yaml` and wired in `Application.kt`.

**Module setup** (`Application.kt`):
- Installs `ContentNegotiation` with `kotlinx.serialization` JSON
- Registers Koin DI — injects `HttpClient` (OkHttp) and `ScrappingService`
- Registers scrapping routes

**Scraping flow**:
1. `GET /scrapping` calls `ScrappingService.scrap()`
2. `ScrappingService` launches `PAScrapper` in a coroutine with a `SupervisorJob`
3. `PAScrapper.scrap()` POSTs to the Pão de Açúcar API and calls `parseProducts()`
4. `parseProducts()` filters by keywords, deduplicates by brand, then dispatches to `parseProductsPerGram()` or uses raw price depending on `QuantityBase`
5. `parseProductsPerGram()` extracts weight from the product name (regex: `500g`, `1 kg`, etc.) and returns price-per-gram

**Price representation**: Sub-cent per-gram prices are stored as scaled integers. Multiply by 10,000 to get millicentavos (`normalizeForMillicent`); divide to recover the real value (`denormalizeFromMillicent`). See `scrapping/docs/database.md` for rationale.

**Database**: PostgreSQL in production; configured via `DB_URL`, `DB_USER`, `DB_PASSWORD` env vars (defaults: `jdbc:postgresql://localhost:5432/my_inflation`, user/pass `salles`). H2 is available as an embedded fallback for tests.

**Dependency injection**: Koin modules are defined in `Application.kt`. New services should be registered there.

**Testing**: Tests use `ktor-server-test-host` and `ktor-client-mock`. See `src/test/kotlin/com/salles/scrapping/PAScrapperTest.kt`.
