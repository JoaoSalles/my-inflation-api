package com.salles.scrapper.scrappers

import com.salles.scrapper.database.TestDatabase
import com.salles.scrapper.data.price.ListProductRequest
import com.salles.scrapper.data.scrap.PASearchRequest
import com.salles.scrapper.data.scrap.PASearchResponse
import com.salles.scrapper.data.productToScrap.ProductToScrapDTO
import com.salles.domain.QuantityBase
import com.salles.scrapper.repositories.PostgresPriceRepository
import com.salles.scrapper.scrapers.PAScrapper
import com.salles.scrapper.services.PriceService
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
            name         = "açúcar",
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
                { "price": 5.99, "name": "Açúcar Cristal 1kg AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "brand": "União", "unitPriceHomogeneousKit": null }
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
            name         = longName,
            search       = "açúcar cristal",
            keyWords     = emptyList<String>(),
            denyWords    = emptyList<String>(),
            quantityBase = QuantityBase.GRAMS,
        ))

        val saved = priceService.list(ListProductRequest()).data
        assertEquals(1, saved.size)
        assertEquals("acucar cristal 1kg aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", saved[0].productLabel)
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
    fun `parseProducts removes price outliers via the IQR rule`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        // All 1kg, so per-gram price == input price * 10 (normalizeForMillicent(price/1000)).
        val products = listOf(
            PASearchResponse(price = 10, name = "Açúcar União 1kg", brand = "União"),
            PASearchResponse(price = 11, name = "Açúcar Camil 1kg", brand = "Camil"),
            PASearchResponse(price = 12, name = "Açúcar Guarani 1kg", brand = "Guarani"),
            PASearchResponse(price = 13, name = "Açúcar Caravelas 1kg", brand = "Caravelas"),
            PASearchResponse(price = 100, name = "Açúcar Premium 1kg", brand = "Premium"),
        )
        val productToScrap = ProductToScrapDTO("açúcar", "açúcar", listOf("acucar"), emptyList(), QuantityBase.GRAMS)

        val result = scrapper.parseProducts(productToScrap, products)

        // per-gram prices: 100, 110, 120, 130, 1000 -> Q1=110, Q3=130, IQR=20, upper fence=160.
        // the 1000 entry exceeds the upper fence and is dropped.
        assertEquals(4, result.size)
        assertTrue(result.none { it.brand == "Premium" }, "outlier above the IQR fence must be removed")
    }

    @Test
    fun `parseProducts keeps every product when none falls outside the IQR fences`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        // per-gram prices 100, 110, 120, 130 are tightly clustered: nothing breaches a fence.
        val products = listOf(
            PASearchResponse(price = 10, name = "Açúcar União 1kg", brand = "União"),
            PASearchResponse(price = 11, name = "Açúcar Camil 1kg", brand = "Camil"),
            PASearchResponse(price = 12, name = "Açúcar Guarani 1kg", brand = "Guarani"),
            PASearchResponse(price = 13, name = "Açúcar Caravelas 1kg", brand = "Caravelas"),
        )
        val productToScrap = ProductToScrapDTO("açúcar", "açúcar", listOf("acucar"), emptyList(), QuantityBase.GRAMS)

        val result = scrapper.parseProducts(productToScrap, products)

        assertEquals(4, result.size, "no product breaches an IQR fence, so none is removed")
    }

    @Test
    fun `parseProducts removes a lone outlier from a three-product sample via the MAD rule`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        // per-gram prices: 100, 110, 1000 -> median 110, MAD 10, fence 110 ± 3*1.4826*10 ≈ [65.5, 154.5].
        // IQR would skip this sample (size < 4), but MAD still flags the 1000.
        val products = listOf(
            PASearchResponse(price = 10, name = "Açúcar União 1kg", brand = "União"),
            PASearchResponse(price = 11, name = "Açúcar Camil 1kg", brand = "Camil"),
            PASearchResponse(price = 100, name = "Açúcar Premium 1kg", brand = "Premium"),
        )
        val productToScrap = ProductToScrapDTO("açúcar", "açúcar", listOf("acucar"), emptyList(), QuantityBase.GRAMS)

        val result = scrapper.parseProducts(productToScrap, products)

        assertEquals(2, result.size)
        assertTrue(result.none { it.brand == "Premium" }, "outlier beyond the MAD fence must be removed")
    }

    @Test
    fun `parseProducts skips outlier filtering for samples smaller than three`() = runTest {
        val scrapper = PAScrapper(HttpClient(MockEngine { respond(ByteReadChannel(""), HttpStatusCode.OK) }) {
            install(ContentNegotiation) { json() }
        })
        // Two products: nothing reliable to compare against, so even a wide gap is kept.
        val products = listOf(
            PASearchResponse(price = 10, name = "Açúcar União 1kg", brand = "União"),
            PASearchResponse(price = 100, name = "Açúcar Premium 1kg", brand = "Premium"),
        )
        val productToScrap = ProductToScrapDTO("açúcar", "açúcar", listOf("acucar"), emptyList(), QuantityBase.GRAMS)

        val result = scrapper.parseProducts(productToScrap, products)

        assertEquals(2, result.size, "below the minimum sample, no filtering is applied")
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
            name         = "café",
            search       = "café torrado",
            keyWords     = emptyList(),
            denyWords    = listOf("+"),
            quantityBase = QuantityBase.GRAMS,
        ))
        assertTrue(result.isEmpty(), "scrap() must not return products filtered out by a denyword")
    }

    @Test
    fun `scrap stores product_label unchanged when productName is shorter than 80 chars`() = runTest {
        val shortName = "acucar cristal 1kg"
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
            name         = shortName,
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
