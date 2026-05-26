package com.salles.core.routes

import com.salles.data.DatabaseException
import com.salles.core.database.TestDatabase
import com.salles.scrapper.data.productToScrap.ProductToScrapCreateResponse
import com.salles.scrapper.data.productToScrap.ProductToScrapDTO
import com.salles.scrapper.repositories.PostgresProductToScrapRepository
import com.salles.scrapper.repositories.ProductToScrapRepository
import com.salles.domain.QuantityBase
import com.salles.scrapper.services.ProductToScrapService
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
            setBody("""{"name":"Açúcar","search":"açúcar cristal","quantityBase":"GRAMS","keyWords":["açúcar","cristal"]}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `GET products returns 200 with deduplicated list`() = testApp {
        client.post("/product") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"name":"Açúcar","search":"açúcar cristal","quantityBase":"GRAMS","keyWords":["cristal"],"denyWords":[]}""")
        }
        client.post("/product") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"name":"Açúcar","search":"açúcar refinado","quantityBase":"GRAMS","keyWords":["refinado"],"denyWords":[]}""")
        }
        val postReponse = client.post("/product") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"name":"Azeite","search":"azeite oliva","quantityBase":"MILLILITERS","keyWords":["azeite"],"denyWords":[]}""")
        }

        val response = client.get("/product")

        assertEquals(HttpStatusCode.Created, postReponse.status)
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"]!!.jsonArray
        assertEquals(2, body.size)
        val names = body.map { it.jsonObject["name"]!!.jsonPrimitive.content }.toSet()
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
            setBody("""{"name":"Açúcar","search":"açúcar","quantityBase":"GRAMS","keyWords":[]}""")
        }
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }
}

private class ThrowingProductToScrapRepository(private val ex: Exception) : ProductToScrapRepository {
    override suspend fun create(productName: String, search: String, quantityBase: QuantityBase, keyWords: List<String>, denyWords: List<String>): ProductToScrapCreateResponse =
        throw ex
    override suspend fun update(id: Long, productName: String, search: String, quantityBase: QuantityBase, keyWords: List<String>, denyWords: List<String>) =
        TODO("not needed")
    override suspend fun list(product: String?): Pair<List<ProductToScrapDTO>, Boolean> = Pair(emptyList(), false)
    override suspend fun listDistinct(): Pair<List<ProductToScrapCreateResponse>, Boolean> = Pair(emptyList(), false)
}
