package com.salles.scrapping.scrappers

import com.salles.scrapping.data.PASearchRequest
import com.salles.scrapping.data.PASearchResponse
import com.salles.scrapping.data.ProductToScrap
import com.salles.scrapping.db.entities.ProductToScrapEntity
import com.salles.scrapping.domain.QuantityBase
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

        PAScrapper(client).scrap(ProductToScrapEntity(
            0,
            "arroz",
            QuantityBase.GRAMS,
            emptyList(),
            emptyList()
        ))

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

        PAScrapper(client).scrap(ProductToScrapEntity(
            0,
            "arroz",
            QuantityBase.GRAMS,
            emptyList(),
            emptyList()
        ))

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

        val scrappedValue = PAScrapper(client).scrap(ProductToScrapEntity(
            0,
            "arroz",
            QuantityBase.GRAMS,
            emptyList(),
            emptyList()
        ))

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

        val result = PAScrapper(client).scrap(ProductToScrapEntity(
            0,
            "açúcar",
            QuantityBase.GRAMS,
            emptyList(),
            emptyList()
        ))

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

        val result = PAScrapper(client).scrap(ProductToScrapEntity(
            0,
            "arroz",
            QuantityBase.GRAMS,
            emptyList(),
            emptyList()
        ))

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

        val result = PAScrapper(client).scrap(ProductToScrapEntity(
            0,
            "arroz",
            QuantityBase.GRAMS,
            emptyList(),
            emptyList()
        ))

        assert(result.isEmpty())
    }

    @Test
    fun `parseProductsPerUnits returns raw price for singular Unidade`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 599, name = "Detergente 1 Unidade", brand = "Brand")
        val productToScrap = ProductToScrap("detergente", emptyList(), emptyList(), QuantityBase.UNITS)
        assertEquals(599, scrapper.parseProductsPerUnits(productToScrap, product))
    }

    @Test
    fun `parseProductsPerUnits returns raw price for plural Unidades`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 1299, name = "Sabão em Pó 10 Unidades", brand = "Brand")
        val productToScrap = ProductToScrap("sabão", emptyList(), emptyList(), QuantityBase.UNITS)
        // price=1299 centavos / 10 units = 129 per unit
        assertEquals(129, scrapper.parseProductsPerUnits(productToScrap, product))
    }

    @Test
    fun `parseProductsPerUnits returns raw price for lowercase unidade`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 399, name = "Esponja 1 unidade", brand = "Brand")
        val productToScrap = ProductToScrap("esponja", emptyList(), emptyList(), QuantityBase.UNITS)
        assertEquals(399, scrapper.parseProductsPerUnits(productToScrap, product))
    }

    @Test
    fun `parseProductsPerUnits returns raw price for lowercase unidades`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 799, name = "Rolo de Papel 6 unidades", brand = "Brand")
        val productToScrap = ProductToScrap("papel", emptyList(), emptyList(), QuantityBase.UNITS)
        // price=799 centavos / 6 units = 133 per unit (integer division)
        assertEquals(133, scrapper.parseProductsPerUnits(productToScrap, product))
    }

    @Test
    fun `parseProductsPerUnits returns 0 when no Unidade pattern found`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 599, name = "Detergente Liquido 500ml", brand = "Brand")
        val productToScrap = ProductToScrap("detergente", emptyList(), emptyList(), QuantityBase.UNITS)
        assertEquals(0, scrapper.parseProductsPerUnits(productToScrap, product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts ml lowercase`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 800, name = "Suco de laranja 800ml", brand = "Brand")
        val productToScrap = ProductToScrap("suco", emptyList(), emptyList(), QuantityBase.MILLILITERS)
        // price=800 centavos, volume=800ml → 800/800=1.0 → normalizeForMillicent(1.0)=10000
        assertEquals(10000, scrapper.parseProductsPerMilliliters(productToScrap, product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts ML uppercase`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 800, name = "Suco de laranja 800ML", brand = "Brand")
        val productToScrap = ProductToScrap("suco", emptyList(), emptyList(), QuantityBase.MILLILITERS)
        assertEquals(10000, scrapper.parseProductsPerMilliliters(productToScrap, product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts ml with space`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 800, name = "Suco de laranja 800 ml", brand = "Brand")
        val productToScrap = ProductToScrap("suco", emptyList(), emptyList(), QuantityBase.MILLILITERS)
        assertEquals(10000, scrapper.parseProductsPerMilliliters(productToScrap, product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts L uppercase`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 400, name = "Leite Integral 1L", brand = "Brand")
        val productToScrap = ProductToScrap("leite", emptyList(), emptyList(), QuantityBase.MILLILITERS)
        // price=400 centavos, volume=1L=1000ml → 400/1000=0.4 → normalizeForMillicent(0.4)=4000
        assertEquals(4000, scrapper.parseProductsPerMilliliters(productToScrap, product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts l lowercase`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 400, name = "Leite Integral 1l", brand = "Brand")
        val productToScrap = ProductToScrap("leite", emptyList(), emptyList(), QuantityBase.MILLILITERS)
        assertEquals(4000, scrapper.parseProductsPerMilliliters(productToScrap, product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts L with space`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 400, name = "Leite Integral 1 L", brand = "Brand")
        val productToScrap = ProductToScrap("leite", emptyList(), emptyList(), QuantityBase.MILLILITERS)
        assertEquals(4000, scrapper.parseProductsPerMilliliters(productToScrap, product))
    }

    @Test
    fun `parseProductsPerMilliliters returns 0 when no volume found`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 400, name = "Leite Integral", brand = "Brand")
        val productToScrap = ProductToScrap("leite", emptyList(), emptyList(), QuantityBase.MILLILITERS)
        assertEquals(0, scrapper.parseProductsPerMilliliters(productToScrap, product))
    }

    @Test
    fun `parseProductsPerMilliliters handles decimal liters`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 600, name = "Suco de uva 1,5L", brand = "Brand")
        val productToScrap = ProductToScrap("suco", emptyList(), emptyList(), QuantityBase.MILLILITERS)
        // 1.5L = 1500ml → 600/1500=0.4 → normalizeForMillicent(0.4)=4000
        assertEquals(4000, scrapper.parseProductsPerMilliliters(productToScrap, product))
    }
}