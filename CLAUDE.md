# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
./gradlew :core:run

# Run all tests
./gradlew test

# Run a single test class
./gradlew :core:test --tests "com.salles.core.SomeTest"

# Build the project
./gradlew build

# Build a fat JAR (output: core/build/libs/core-all.jar)
./gradlew :core:buildFatJar
```

## Project Structure

This is a multi-module Gradle project. The modules are:

```
core/       — executable entry point; wires all modules together via Koin DI
api/        — HTTP layer: routes, services, repositories, DTOs for price and productToScrap features
scrapper/   — scraping feature: PAScrapper, ScrappingService, routes, repositories, DTOs
domain/     — interfaces and value types only (no implementation)
data/       — database: PostgresDatabaseFactory, Exposed table definitions
```

### core
```
core/src/main/kotlin/com/salles/core/
├── Application.kt              # Ktor module: installs ContentNegotiation, CORS, CloudflareValidation, Koin DI; registers all routes
└── plugins/
    └── CloudflareValidation.kt # Ktor plugin: validates CF-Secret header when CLOUDFLARE_SECRET env var is set
core/src/main/resources/
├── application.yaml
└── logback.xml
```

### api
```
api/src/main/kotlin/com/salles/api/
├── data/
│   ├── price/                  # CreatePriceCommand, ListPriceRequest, PriceAVGResponse, PriceDTO
│   ├── productToScrap/         # ProductToScrapCreateResponse, ProductToScrapDTO, ProductToScrapResponse
│   └── PagedResponse.kt
├── repositories/
│   ├── PriceRepository.kt      # PostgresPriceRepository (Exposed)
│   └── ProductToScrapRepository.kt
├── routes/
│   ├── PriceRoutes.kt
│   └── ProductToScrapRoutes.kt
└── services/
    ├── PriceService.kt
    └── ProductToScrapService.kt
```

### scrapper
```
scrapper/src/main/kotlin/com/salles/scrapper/
├── data/scrap/                 # PASearchRequest, PASearchResponse, ScrapRequest
├── repositories/               # PriceRepository, ProductToScrapRepository (Exposed)
├── routes/
│   └── ScrappingRoutes.kt      # GET /scrapping
├── scrapers/
│   └── PAScrapper.kt           # Pão de Açúcar scraper; filters by keyword/brand, computes per-unit price
├── services/
│   ├── ScrappingService.kt     # Launches scrapers with a SupervisorJob coroutine scope
│   ├── PriceService.kt
│   └── ProductToScrapService.kt
└── utils/
    └── MillicentUtils.kt       # normalizeForMillicent / denormalizeFromMillicent
```

### domain
```
domain/src/main/kotlin/com/salles/domain/
├── price/                      # CreatePriceRequest, ListProductPriceRequestInterface, PriceAvgInterface, PriceInterface
├── productToScrap/
│   └── ProductToScrap.kt       # Interface: name, keywords, quantity base
├── repositories/
│   └── PriceRepositoryInterface.kt
├── scrapper/
│   ├── PASearchResponseInterface.kt
│   └── ScrapperInterface.kt    # Generic Scrapper<T> interface
├── services/
│   └── PriceServiceInterface.kt
├── PagedResponseInterface.kt
├── QuantityBase.kt             # Enum: GRAMS, UNITS, MILLILITERS
└── SearchResponse.kt           # Interface: price (Int?), name (String)
```

### data
```
data/src/main/kotlin/com/salles/data/
├── tables/
│   ├── Price.kt                # Exposed table definition
│   └── ProductsToScrap.kt
├── DatabaseExceptions.kt
└── PostgresDatabaseFactory.kt  # Connects via DB_URL, DB_USER, DB_PASSWORD env vars; runs Flyway migrations
```

## Architecture

**Entry point**: `core/Application.kt` — registers all routes and wires all dependencies via Koin.

**Module dependency graph**:
```
core → api, scrapper, domain, data
api  → domain, data
scrapper → domain, data
data → (none)
domain → (none)
```

**Scraping flow**:
1. `GET /scrapping` calls `ScrappingService.scrap()`
2. `ScrappingService` launches `PAScrapper` in a coroutine with a `SupervisorJob`
3. `PAScrapper.scrap()` POSTs to the Pão de Açúcar API and calls `parseProducts()`
4. `parseProducts()` filters by keywords, deduplicates by brand, then dispatches to `parseProductsPerGram()` or uses raw price depending on `QuantityBase`
5. `parseProductsPerGram()` extracts weight from the product name (regex: `500g`, `1 kg`, etc.) and returns price-per-gram

**Price representation**: Sub-cent per-gram prices are stored as scaled integers. Multiply by 10,000 to get millicentavos (`normalizeForMillicent`); divide to recover the real value (`denormalizeFromMillicent`).

**Database**: PostgreSQL in production; configured via `DB_URL`, `DB_USER`, `DB_PASSWORD` env vars. Migrations managed by Flyway (migration files in `data/src/main/resources`).

**Dependency injection**: Koin modules are defined in `core/Application.kt`. New services must be registered there.

**Deployment**: Docker builds `core-all.jar` via `gradle :core:buildFatJar`. See `Dockerfile`.
