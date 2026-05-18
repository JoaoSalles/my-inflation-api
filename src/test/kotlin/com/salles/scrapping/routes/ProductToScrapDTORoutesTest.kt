package com.salles.scrapping.routes

import com.salles.scrapping.db.DatabaseException
import com.salles.database.TestDatabase
import com.salles.scrapping.domain.ProductToScrap
import com.salles.scrapping.repositories.PostgresProductToScrapRepository
import com.salles.scrapping.repositories.ProductToScrapRepository
import com.salles.scrapping.domain.QuantityBase
import com.salles.scrapping.services.ProductToScrapService
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

class ProductToScrapDTORoutesTest {

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
        val response = client.post("/product") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"productName":"Açúcar","search":"açúcar cristal","quantityBase":"GRAMS","keyWords":["açúcar","cristal"]}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `GET products returns 200 with deduplicated list`() = testApp {
        client.post("/product") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"productName":"Açúcar","search":"açúcar cristal","quantityBase":"GRAMS","keyWords":["cristal"],"denyWords":[]}""")
        }
        client.post("/product") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"productName":"Açúcar","search":"açúcar refinado","quantityBase":"GRAMS","keyWords":["refinado"],"denyWords":[]}""")
        }
        client.post("/product") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"productName":"Azeite","search":"azeite oliva","quantityBase":"MILLILITERS","keyWords":["azeite"],"denyWords":[]}""")
        }

        val response = client.get("/product")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"]!!.jsonArray
        assertEquals(2, body.size)
        val names = body.map { it.jsonObject["productName"]!!.jsonPrimitive.content }.toSet()
        assertEquals(setOf("Açúcar", "Azeite"), names)
    }

    @Test
    fun `GET products returns 200 with empty list when no products exist`() = testApp {
        val response = client.get("/product")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"]!!.jsonArray
        assertEquals(0, body.size)
    }

    @Test
    fun `POST products returns 500 on generic database error`() = testApp(
        ThrowingProductToScrapRepository(DatabaseException(RuntimeException("connection lost")))
    ) {
        val response = client.post("/product") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"productName":"Açúcar","search":"açúcar","quantityBase":"GRAMS","keyWords":[]}""")
        }
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }
}

private class ThrowingProductToScrapRepository(private val ex: Exception) : ProductToScrapRepository {
    override suspend fun create(productName: String, search: String, quantityBase: QuantityBase, keyWords: List<String>, denyWords: List<String>): ProductToScrap =
        throw ex
    override suspend fun update(id: Long, productName: String, search: String, quantityBase: QuantityBase, keyWords: List<String>, denyWords: List<String>) =
        TODO("not needed")
    override suspend fun list(product: String?): Pair<List<ProductToScrap>, Boolean> = Pair(emptyList(), false)
    override suspend fun listDistinct(): Pair<List<ProductToScrap>, Boolean> = Pair(emptyList(), false)
}
