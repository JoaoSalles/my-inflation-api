---
name: ktor-modular-structure
description: Creates feature-based modular structure for Ktor projects. Use this skill whenever a new module or feature needs to be scaffolded, a subfolder needs to be added to an existing module, or any time the user says things like "create a module for X", "add a users module", "scaffold a new feature", "add routes/service/repository for X", "create the cities module", "I need a new feature module". Always trigger when the user is working in a Ktor project and wants to add any layer (domain, data, repositories, service, routes, db) to a feature.
---

# Ktor Modular Structure

Scaffolds feature-based modules for Ktor projects. Each module is a self-contained feature package with consistent layers wired into Koin and the Ktor application config.

## Module Layout

Every feature lives under `src/main/kotlin/<basePackage>/<moduleName>/`:

```
<moduleName>/
├── domain/         # Interfaces (repository and service contracts)
├── data/           # Entity classes, table schemas (SQL constants), DTOs
├── repositories/   # Data access layer — implements domain interfaces
├── services/        # Business logic — implements domain interfaces
├── routes/         # Ktor routing extension functions
└── db/             # Database connection and initialization
```

The base package for this project is `com.salles`.

---

## UUID Setup (required once per project)

### Dependency (`build.gradle.kts`)

```kotlin
implementation("com.fasterxml.uuid:java-uuid-generator:5.1.0")
```

### UUID Serializer (shared utility — create once at `com.salles.common.UUIDSerializer.kt`)

```kotlin
package com.salles.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}
```

### ContentNegotiation (in `configureSerialization`)

Register the serializer so every module's `@Contextual UUID` fields serialize correctly:

```kotlin
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

install(ContentNegotiation) {
    json(Json {
        serializersModule = SerializersModule {
            contextual(UUID::class, UUIDSerializer)
        }
    })
}
```

---

## Creating a Full Module

Replace `<ModuleName>` with PascalCase (e.g., `City`) and `<moduleName>` with camelCase (e.g., `city`) throughout.

### domain/

**`<ModuleName>Repository.kt`** — data access contract
```kotlin
package com.salles.<moduleName>.domain

import com.salles.<moduleName>.data.<ModuleName>
import java.util.UUID

interface <ModuleName>Repository {
    suspend fun findById(id: UUID): <ModuleName>?
    suspend fun create(entity: <ModuleName>): UUID
    suspend fun update(id: UUID, entity: <ModuleName>)
    suspend fun delete(id: UUID)
}
```

**`<ModuleName>Service.kt`** — business logic contract
```kotlin
package com.salles.<moduleName>.domain

import com.salles.<moduleName>.data.<ModuleName>
import java.util.UUID

interface <ModuleName>Service {
    suspend fun getById(id: UUID): <ModuleName>
    suspend fun create(entity: <ModuleName>): UUID
    suspend fun update(id: UUID, entity: <ModuleName>)
    suspend fun delete(id: UUID)
}
```

### data/

**`<ModuleName>.kt`** — entity / serializable model
```kotlin
package com.salles.<moduleName>.data

import com.salles.common.UUIDSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class <ModuleName>(
    @Contextual val id: UUID? = null
    // Add fields here
)
```

**`<ModuleName>Table.kt`** — SQL schema constants (raw JDBC)
```kotlin
package com.salles.<moduleName>.data

object <ModuleName>Table {
    const val CREATE = """
        CREATE TABLE IF NOT EXISTS <moduleName>s (
            id UUID PRIMARY KEY
            -- Add columns here
        )
    """
    const val SELECT_BY_ID = "SELECT * FROM <moduleName>s WHERE id = ?"
    const val INSERT = "INSERT INTO <moduleName>s (id, ...) VALUES (?, ?)"
    const val UPDATE = "UPDATE <moduleName>s SET ... WHERE id = ?"
    const val DELETE = "DELETE FROM <moduleName>s WHERE id = ?"
}
```

**`<ModuleName>Dto.kt`** — request/response shapes
```kotlin
package com.salles.<moduleName>.data

import kotlinx.serialization.Serializable

@Serializable
data class Create<ModuleName>Request(
    // Add request fields here
)
```

### repositories/

**`<ModuleName>RepositoryImpl.kt`**
```kotlin
package com.salles.<moduleName>.repositories

import com.fasterxml.uuid.Generators
import com.salles.<moduleName>.data.<ModuleName>
import com.salles.<moduleName>.data.<ModuleName>Table
import com.salles.<moduleName>.domain.<ModuleName>Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.util.UUID

private val uuidV7Generator = Generators.timeBasedEpochGenerator()

class <ModuleName>RepositoryImpl(private val connection: Connection) : <ModuleName>Repository {

    init {
        connection.createStatement().executeUpdate(<ModuleName>Table.CREATE)
    }

    override suspend fun create(entity: <ModuleName>): UUID = withContext(Dispatchers.IO) {
        val id = uuidV7Generator.generate()
        val stmt = connection.prepareStatement(<ModuleName>Table.INSERT)
        stmt.setObject(1, id)
        // bind remaining parameters
        stmt.executeUpdate()
        id
    }

    override suspend fun findById(id: UUID): <ModuleName>? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(<ModuleName>Table.SELECT_BY_ID)
        stmt.setObject(1, id)
        val rs = stmt.executeQuery()
        if (rs.next()) <ModuleName>(/* map columns */) else null
    }

    override suspend fun update(id: UUID, entity: <ModuleName>) = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(<ModuleName>Table.UPDATE)
        // bind parameters
        stmt.setObject(/* last param index */, id)
        stmt.executeUpdate()
    }

    override suspend fun delete(id: UUID) = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(<ModuleName>Table.DELETE)
        stmt.setObject(1, id)
        stmt.executeUpdate()
    }
}
```

### service/

**`<ModuleName>ServiceImpl.kt`**
```kotlin
package com.salles.<moduleName>.service

import com.salles.<moduleName>.data.<ModuleName>
import com.salles.<moduleName>.domain.<ModuleName>Repository
import com.salles.<moduleName>.domain.<ModuleName>Service
import java.util.UUID

class <ModuleName>ServiceImpl(private val repository: <ModuleName>Repository) : <ModuleName>Service {

    override suspend fun getById(id: UUID): <ModuleName> =
        repository.findById(id) ?: throw NoSuchElementException("<ModuleName> $id not found")

    override suspend fun create(entity: <ModuleName>): UUID = repository.create(entity)

    override suspend fun update(id: UUID, entity: <ModuleName>) = repository.update(id, entity)

    override suspend fun delete(id: UUID) = repository.delete(id)
}
```

### routes/

**`<moduleName>Routes.kt`**
```kotlin
package com.salles.<moduleName>.routes

import com.salles.<moduleName>.data.<ModuleName>
import com.salles.<moduleName>.domain.<ModuleName>Service
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.UUID

fun Application.configure<ModuleName>Routes() {
    val service: <ModuleName>Service by inject()
    routing {
        route("/<moduleName>") {
            post {
                val body = call.receive<<ModuleName>>()
                val id = service.create(body)
                call.respond(HttpStatusCode.Created, id.toString())
            }
            get("/{id}") {
                val id = UUID.fromString(call.parameters["id"] ?: throw IllegalArgumentException("Missing ID"))
                call.respond(service.getById(id))
            }
            put("/{id}") {
                val id = UUID.fromString(call.parameters["id"] ?: throw IllegalArgumentException("Missing ID"))
                service.update(id, call.receive())
                call.respond(HttpStatusCode.OK)
            }
            delete("/{id}") {
                val id = UUID.fromString(call.parameters["id"] ?: throw IllegalArgumentException("Missing ID"))
                service.delete(id)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
```

### db/

**`<moduleName>Database.kt`**
```kotlin
package com.salles.<moduleName>.db

import io.ktor.server.application.*
import java.sql.Connection
import java.sql.DriverManager

fun Application.connect<ModuleName>Database(embedded: Boolean = true): Connection {
    Class.forName("org.postgresql.Driver")
    return if (embedded) {
        log.info("Using embedded H2 for <moduleName> (testing)")
        DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "root", "")
    } else {
        val url = environment.config.property("postgres.url").getString()
        val user = environment.config.property("postgres.user").getString()
        val password = environment.config.property("postgres.password").getString()
        DriverManager.getConnection(url, user, password)
    }
}
```

---

## Wiring a New Module

### Koin.kt

Add the repository and service bindings inside the existing `modules(module { ... })` block:

```kotlin
// <ModuleName> module
single<<ModuleName>Repository> {
    val connection = get<Application>().connect<ModuleName>Database()
    <ModuleName>RepositoryImpl(connection)
}
single<<ModuleName>Service> { <ModuleName>ServiceImpl(get()) }
```

If the `Connection` is shared (one DB connection for all modules), inject it directly:
```kotlin
single<<ModuleName>Repository> { <ModuleName>RepositoryImpl(get()) }
```

### application.yaml

Add the routes module to the modules list:

```yaml
ktor:
  application:
    modules:
      # existing modules...
      - com.salles.<moduleName>.routes.<ModuleName>RoutesKt.configure<ModuleName>Routes
```

The routes function uses Koin's `by inject()` so it takes no parameters — it matches the zero-arg module declaration format Ktor expects.

---

## Adding a Single Subfolder

When only one layer is needed for an existing module, create only that folder and its file, update the package declaration, then:
- Adding `repositories/` or `service/`: register the new class in Koin.kt
- Adding `routes/`: register in application.yaml and use `by inject()` for dependencies
- Adding `domain/`, `data/`, or `db/`: no wiring changes needed unless new services are exposed

## YOU MUST NOT

Delete other files or change structures already created.
