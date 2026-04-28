package com.salles.scrapping.scrappers

import com.salles.scrapping.data.PASearchRequest
import com.salles.scrapping.scrapers.PAScrapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PAScrapperTest {

    @Test
    fun `scrap posts to correct URL`() = runTest {
        var capturedUrl = ""
        var capturedMethod = HttpMethod.Get

        val client = HttpClient(MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedMethod = request.method
            respond(
                content = ByteReadChannel("{}"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        PAScrapper(client).scrap("arroz")

        assertEquals("https://api.vendas.gpa.digital/pa/search/search", capturedUrl)
        assertEquals(HttpMethod.Post, capturedMethod)
    }

    @Test
    fun `scrap sends correct JSON body`() = runTest {
        var capturedBody = ""

        val client = HttpClient(MockEngine { request ->
            capturedBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respond(
                content = ByteReadChannel("{}"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        PAScrapper(client).scrap("arroz")

        val parsed = Json.decodeFromString<PASearchRequest>(capturedBody)
        assertEquals("arroz", parsed.terms)
        assertEquals(1, parsed.page)
        assertEquals("relevance", parsed.sortBy)
        assertEquals(21, parsed.resultsPerPage)
        assertEquals(true, parsed.allowRedirect)
        assertEquals(461, parsed.storeId)
        assertEquals("ecom", parsed.department)
        assertEquals("fallback", parsed.partner)
    }

    @Test
    fun `scrap should return empty list and do not throw on HTTP error`() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.InternalServerError
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val scrappedValue = PAScrapper(client).scrap("arroz")

        assert(scrappedValue.isEmpty())
    }

    @Test
    fun `scrap returns correct products from valid 200 response`() = runTest {
        val json = """
            {
              "products": [
                {
                  "price": 5.99,
                  "name": "Açúcar Refinado 1kg",
                  "brand": "União",
                  "unitPriceHomogeneousKit": null
                },
                {
                  "price": 3.49,
                  "name": "Arroz Branco 5kg",
                  "brand": "Camil",
                  "unitPriceHomogeneousKit": 1.75
                }
              ]
            }
        """.trimIndent()

        val client = HttpClient(MockEngine {
            respond(
                content = ByteReadChannel(json),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val result = PAScrapper(client).scrap("açúcar")

        assertEquals(1, result.size)
        assertEquals("acucar refinado 1kg", result[0].name)
        assertEquals(599, result[0].price)
        assertEquals("União", result[0].brand)
    }

    @Test
    fun `scrap returns empty list and does not throw on HTTP 501`() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.NotImplemented
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val result = PAScrapper(client).scrap("arroz")

        assert(result.isEmpty())
    }

    @Test
    fun `scrap returns empty list when products array is empty`() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = ByteReadChannel("""{"products": []}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val result = PAScrapper(client).scrap("arroz")

        assert(result.isEmpty())
    }
}