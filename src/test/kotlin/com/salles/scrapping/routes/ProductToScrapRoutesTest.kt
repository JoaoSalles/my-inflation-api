package com.salles.scrapping.routes

import com.salles.scrapping.db.DatabaseException
import com.salles.database.TestDatabase
import com.salles.scrapping.db.entities.ProductToScrapEntity
import com.salles.scrapping.repositories.PostgresProductToScrapRepository
import com.salles.scrapping.repositories.ProductToScrapRepository
import com.salles.scrapping.domain.QuantityBase
import com.salles.scrapping.services.ProductToScrapService
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductToScrapRoutesTest {

    @BeforeTest
    fun setUp() {
        TestDatabase.reset()
    }

    private fun testApp(
        repository: ProductToScrapRepository = PostgresProductToScrapRepository(),
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        application {
            install(ContentNegotiation) { json() }
            install(Koin) {
                modules(module {
                    single<ProductToScrapRepository> { repository }
                    single { ProductToScrapService(get()) }
                })
            }
            productToScrapRoutes()
        }
        block()
    }

    @Test
    fun `POST products returns 201 with entity on success`() = testApp {
        val response = client.post("/products") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"productName":"Açúcar","quantityBase":"GRAMS","keyWords":["açúcar","cristal"]}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST products returns 409 when product name already exists`() = testApp {
        val body = """{"productName":"Açúcar","quantityBase":"GRAMS","keyWords":[]}"""
        client.post("/products") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(body)
        }
        val response = client.post("/products") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST products returns 500 on generic database error`() = testApp(
        ThrowingProductToScrapRepository(DatabaseException(RuntimeException("connection lost")))
    ) {
        val response = client.post("/products") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"productName":"Açúcar","quantityBase":"GRAMS","keyWords":[]}""")
        }
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }
}

private class ThrowingProductToScrapRepository(private val ex: Exception) : ProductToScrapRepository {
    override suspend fun create(productName: String, quantityBase: QuantityBase, keyWords: List<String>): ProductToScrapEntity =
        throw ex
    override suspend fun update(id: Long, productName: String, quantityBase: QuantityBase, keyWords: List<String>) =
        TODO("not needed")
    override suspend fun list(): List<ProductToScrapEntity> = emptyList()
}
