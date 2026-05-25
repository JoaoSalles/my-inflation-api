package com.salles.scrapping.scrappers

import com.salles.database.TestDatabase
import com.salles.scrapping.data.price.ListProductRequest
import com.salles.scrapping.data.scrap.PASearchRequest
import com.salles.scrapping.data.scrap.PASearchResponse
import com.salles.scrapping.data.productToScrap.ProductToScrapDTO
import com.salles.scrapping.domain.QuantityBase
import com.salles.scrapping.repositories.PostgresPriceRepository
import com.salles.scrapping.scrapers.PAScrapper
import com.salles.scrapping.services.PriceService
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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PAScrapperTest {

    @BeforeTest
    fun setUp() {
        TestDatabase.reset()
    }

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

        PAScrapper(client).scrap(ProductToScrapDTO(
            "arroz",
            "arroz",
            emptyList<String>(),
            denyWords = emptyList<String>(),
            QuantityBase.GRAMS,
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

        PAScrapper(client).scrap(ProductToScrapDTO(
            "arroz",
            "arroz",
            emptyList<String>(),
            emptyList<String>(),
            QuantityBase.GRAMS,
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

        val scrappedValue = PAScrapper(client).scrap(ProductToScrapDTO(
            "arroz",
            "arroz",
            emptyList<String>(),
            emptyList<String>(),
            QuantityBase.GRAMS,
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

        val result = PAScrapper(client).scrap(ProductToScrapDTO(
            "açúcar",
            "açúcar",
            emptyList<String>(),
            emptyList<String>(),
            QuantityBase.GRAMS
        ))

        assertEquals(1, result.size)
        assertEquals("acucar refinado 1kg", result[0].name)
        assertEquals(5990, result[0].price)
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

        val result = PAScrapper(client).scrap(ProductToScrapDTO(
            "arroz",
            "arroz",
            emptyList<String>(),
            emptyList<String>(),
            QuantityBase.GRAMS,
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

        val result = PAScrapper(client).scrap(ProductToScrapDTO(
            "arroz",
            "arroz",
            emptyList<String>(),
            emptyList<String>(),
            quantityBase = QuantityBase.GRAMS,
        ))

        assert(result.isEmpty())
    }

    @Test
    fun `parseProductsPerUnits returns raw price for singular Unidade`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 599, name = "Detergente 1 Unidade", brand = "Brand")
        assertEquals(5990, scrapper.parseProductsPerUnits(product))
    }

    @Test
    fun `parseProductsPerUnits returns raw price for plural Unidades`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 1299, name = "Sabão em Pó 10 Unidades", brand = "Brand")
        // price=1299 centavos / 10 units * 10 = 1290
        assertEquals(1290, scrapper.parseProductsPerUnits(product))
    }

    @Test
    fun `parseProductsPerUnits returns raw price for lowercase unidade`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 399, name = "Esponja 1 unidade", brand = "Brand")
        assertEquals(3990, scrapper.parseProductsPerUnits(product))
    }

    @Test
    fun `parseProductsPerUnits returns raw price for lowercase unidades`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 799, name = "Rolo de Papel 6 unidades", brand = "Brand")
        // price=799 centavos / 6 units = 133 per unit (integer division)
        assertEquals(1330, scrapper.parseProductsPerUnits(product))
    }

    @Test
    fun `parseProductsPerUnits returns 0 when no Unidade pattern found`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 599, name = "Detergente Liquido 500ml", brand = "Brand")
        assertEquals(1, scrapper.parseProductsPerUnits(product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts ml lowercase`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 800, name = "Suco de laranja 800ml", brand = "Brand")
        // price=800 centavos, volume=800ml → 800/800=1.0 → normalizeForMillicent(1.0)=10000
        assertEquals(10000, scrapper.parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts ML uppercase`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 800, name = "Suco de laranja 800ML", brand = "Brand")
        assertEquals(10000, scrapper.parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts ml with space`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 800, name = "Suco de laranja 800 ml", brand = "Brand")
        assertEquals(10000, scrapper.parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts L uppercase`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 400, name = "Leite Integral 1L", brand = "Brand")
        // price=400 centavos, volume=1L=1000ml → 400/1000=0.4 → normalizeForMillicent(0.4)=4000
        assertEquals(4000, scrapper.parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts l lowercase`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 400, name = "Leite Integral 1l", brand = "Brand")
        assertEquals(4000, scrapper.parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts L with space`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 400, name = "Leite Integral 1 L", brand = "Brand")
        assertEquals(4000, scrapper.parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters returns 0 when no volume found`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 400, name = "Leite Integral", brand = "Brand")
        assertEquals(0, scrapper.parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters handles decimal liters`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(price = 600, name = "Suco de uva 1,5L", brand = "Brand")
        // 1.5L = 1500ml → 600/1500=0.4 → normalizeForMillicent(0.4)=4000
        assertEquals(4000, scrapper.parseProductsPerMilliliters(product))
    }

    @Test
    fun `scrap saves one price row per parsed product`() = runTest {
        val json = """
            {
              "products": [
                { "price": 5.99, "name": "Açúcar Cristal 1kg",  "brand": "União",     "unitPriceHomogeneousKit": null },
                { "price": 7.49, "name": "Açúcar Cristal 2kg",  "brand": "Caravelas", "unitPriceHomogeneousKit": null },
                { "price": 3.00, "name": "Açúcar kit",          "brand": "Caravelas", "unitPriceHomogeneousKit": null }
              ]
            }
        """.trimIndent()

        val client = HttpClient(MockEngine {
            respond(
                content = ByteReadChannel(json),
                status  = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) { install(ContentNegotiation) { json() } }

        val priceService = PriceService(PostgresPriceRepository())
        val scrapper = PAScrapper(client, priceService)

        scrapper.scrap(ProductToScrapDTO(
            name  = "açúcar",
            search       = "açúcar cristal",
            keyWords     = listOf("cristal"),
            denyWords    = emptyList<String>(),
            quantityBase = QuantityBase.GRAMS,
        ))

        val saved = priceService.list(ListProductRequest()).data
        // third product has unitPriceHomogeneousKit ≠ null → filtered out before parsing
        // first two have different brands → both saved
        assertEquals(2, saved.size)
        val brands = saved.map { it.brand }.toSet()
        assertTrue("União" in brands)
        assertTrue("Caravelas" in brands)
        assertTrue(saved.all { it.name == "açúcar" })
        assertTrue(saved.all { it.quantityBase == QuantityBase.GRAMS })
        assertTrue(saved.all { it.price > 0 })
    }

    @Test
    fun `scrap stores product_label as first 80 chars of productName`() = runTest {
        val longName = "A".repeat(100)
        val json = """
            {
              "products": [
                { "price": 5.99, "name": "Açúcar Cristal 1kg", "brand": "União", "unitPriceHomogeneousKit": null }
              ]
            }
        """.trimIndent()

        val client = HttpClient(MockEngine {
            respond(
                content = ByteReadChannel(json),
                status  = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) { install(ContentNegotiation) { json() } }

        val priceService = PriceService(PostgresPriceRepository())
        val scrapper = PAScrapper(client, priceService)

        scrapper.scrap(ProductToScrapDTO(
            name  = longName,
            search       = "açúcar cristal",
            keyWords     = emptyList<String>(),
            denyWords    = emptyList<String>(),
            quantityBase = QuantityBase.GRAMS,
        ))

        val saved = priceService.list(ListProductRequest()).data
        assertEquals(1, saved.size)
        assertEquals("A".repeat(80), saved[0].productLabel)
    }

    @Test
    fun `parseProducts filters out product whose name contains a denyword`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        val product = PASearchResponse(
            price = 599,
            name = "Compre 1 Café Torrado e Moído Tradicional União 500g  + Café Torrado e Moído Extra Forte  União 500 +  R\$0,01 Leve 1  Açucar Refinado UNIÃO 1Kg",
            brand = "União"
        )
        val productToScrap = ProductToScrapDTO("café", "café", emptyList(), listOf("+"), QuantityBase.GRAMS)

        val result = scrapper.parseProducts(productToScrap, listOf(product))

        assertTrue(result.isEmpty(), "Product containing denyword '+' should be filtered out")
    }

    @Test
    fun `scrap filters out products whose name contains a denyword`() = runTest {
        val json = """
            {
              "products": [
                {
                  "price": 5.99,
                  "name": "Compre 1 Café Torrado e Moído Tradicional União 500g  + Café Torrado e Moído Extra Forte  União 500 +  R${'$'}0,01 Leve 1  Açucar Refinado UNIÃO 1Kg",
                  "brand": "União",
                  "unitPriceHomogeneousKit": null
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
        }) { install(ContentNegotiation) { json() } }

        val result = PAScrapper(client).scrap(ProductToScrapDTO(
            name = "café",
            search = "café torrado",
            keyWords = emptyList(),
            denyWords = listOf("+"),
            quantityBase = QuantityBase.GRAMS,
        ))
        print("result ${result}")
        assertTrue(result.isEmpty(), "scrap() must not return products filtered out by a denyword")
    }

    @Test
    fun `scrap stores product_label unchanged when productName is shorter than 80 chars`() = runTest {
        val shortName = "açúcar"
        val json = """
            {
              "products": [
                { "price": 5.99, "name": "Açúcar Cristal 1kg", "brand": "União", "unitPriceHomogeneousKit": null }
              ]
            }
        """.trimIndent()

        val client = HttpClient(MockEngine {
            respond(
                content = ByteReadChannel(json),
                status  = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) { install(ContentNegotiation) { json() } }

        val priceService = PriceService(PostgresPriceRepository())
        val scrapper = PAScrapper(client, priceService)

        scrapper.scrap(ProductToScrapDTO(
            name  = shortName,
            search       = "açúcar cristal",
            keyWords     = emptyList<String>(),
            denyWords    = emptyList<String>(),
            quantityBase = QuantityBase.GRAMS,
        ))

        val saved = priceService.list(ListProductRequest()).data
        assertEquals(1, saved.size)
        assertEquals(shortName, saved[0].productLabel)
    }
}