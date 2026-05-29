package com.salles.scrapper.scrappers

import com.salles.domain.QuantityBase
import com.salles.scrapper.data.price.ListProductRequest
import com.salles.scrapper.data.productToScrap.ProductToScrapDTO
import com.salles.scrapper.data.scrap.CaSearchResponse
import com.salles.scrapper.database.TestDatabase
import com.salles.scrapper.repositories.PostgresPriceRepository
import com.salles.scrapper.scrapers.CaScrapper
import com.salles.scrapper.services.PriceService
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CaScrapperTest {

    @BeforeTest
    fun setUp() {
        TestDatabase.reset()
    }

    // --- CaSearchResponse ---

    @Test
    fun `CaSearchResponse normalizes name to lowercase without accents`() {
        assertEquals(
            "acucar refinado uniao 1kg",
            CaSearchResponse(price = 379, name = "Açúcar Refinado União 1kg").name
        )
    }

    // --- parsePrice ---

    @Test
    fun `parsePrice parses comma decimal`() = assertEquals(379, CaScrapper().parsePrice("R$ 3,79"))

    @Test
    fun `parsePrice parses thousands separator`() = assertEquals(123456, CaScrapper().parsePrice("R$ 1.234,56"))

    @Test
    fun `parsePrice parses sub-real`() = assertEquals(99, CaScrapper().parsePrice("R$ 0,99"))

    @Test
    fun `parsePrice returns 0 for unparseable`() = assertEquals(0, CaScrapper().parsePrice("indisponível"))

    // --- extractCards (DOM extraction via setContent) ---

    @Test
    fun `extractCards reads title and price from each product card`() {
        val html = """
            <div data-id="product-grid">
              <div data-testid="search-product-card">
                <h2 class="text-sm text-zinc-medium text-left font-normal truncate-text h-[60px]">Açúcar Refinado União 1kg</h2>
                <span class="text-price-default font-bold whitespace-nowrap block min-h-6 text-lg leading-none">R$ 3,79</span>
              </div>
              <div data-testid="search-product-card">
                <h2 class="text-sm text-zinc-medium text-left font-normal truncate-text h-[60px]">Arroz Branco Tio João 5kg</h2>
                <span class="text-price-default font-bold whitespace-nowrap block min-h-6 text-lg leading-none">R$ 24,90</span>
              </div>
            </div>
        """.trimIndent()

        val page = PlaywrightFixture.newPage(html)
        try {
            val cards = CaScrapper().extractCards(page)

            assertEquals(2, cards.size)
            assertEquals("acucar refinado uniao 1kg", cards[0].name)
            assertEquals(379, cards[0].price)
            assertEquals("arroz branco tio joao 5kg", cards[1].name)
            assertEquals(2490, cards[1].price)
        } finally {
            page.close()
        }
    }

    // --- parseProducts (reuses the shared util) ---

    @Test
    fun `parseProducts keeps matching product and sets brand to title truncated to 100 chars`() = runTest {
        val longTitle = "Açúcar " + "X".repeat(200) + " 1kg"
        val products = listOf(CaSearchResponse(price = 999, name = longTitle))
        val productToScrap = ProductToScrapDTO(
            name = "açúcar",
            search = "açúcar",
            keyWords = listOf("acucar"),
            denyWords = emptyList(),
            quantityBase = QuantityBase.GRAMS,
        )

        val result = CaScrapper().parseProducts(productToScrap, products)

        assertEquals(1, result.size)
        assertEquals(9990, result[0].price)
        assertEquals(100, result[0].brand.length)
    }

    @Test
    fun `parseProducts filters out product containing a denyword`() = runTest {
        val products = listOf(CaSearchResponse(price = 999, name = "Açúcar Refinado 1kg + Brinde"))
        val productToScrap = ProductToScrapDTO(
            name = "açúcar",
            search = "açúcar",
            keyWords = emptyList(),
            denyWords = listOf("+"),
            quantityBase = QuantityBase.GRAMS,
        )

        val result = CaScrapper().parseProducts(productToScrap, products)

        assertTrue(result.isEmpty(), "product containing denyword '+' must be filtered out")
    }

    @Test
    fun `parseProducts drops product whose weight cannot be parsed`() = runTest {
        val products = listOf(CaSearchResponse(price = 999, name = "Açúcar Refinado"))
        val productToScrap = ProductToScrapDTO(
            name = "açúcar",
            search = "açúcar",
            keyWords = emptyList(),
            denyWords = emptyList(),
            quantityBase = QuantityBase.GRAMS,
        )

        val result = CaScrapper().parseProducts(productToScrap, products)

        assertTrue(result.isEmpty(), "GRAMS product with no parseable weight yields price 0 and is dropped")
    }

    // --- scrapFromPage (extract -> parse -> persist) ---

    @Test
    fun `scrapFromPage saves one price row per parsed product with Carrefour location and title brand`() = runTest {
        val html = """
            <div data-id="product-grid">
              <div data-testid="search-product-card">
                <h2 class="text-sm">Açúcar Refinado União 1kg</h2>
                <span class="text-price-default">R$ 3,79</span>
              </div>
              <div data-testid="search-product-card">
                <h2 class="text-sm">Açúcar Cristal Caravelas 1kg</h2>
                <span class="text-price-default">R$ 4,29</span>
              </div>
            </div>
        """.trimIndent()

        val priceService = PriceService(PostgresPriceRepository())
        val scrapper = CaScrapper(priceService)

        val page = PlaywrightFixture.newPage(html)
        try {
            scrapper.scrapFromPage(
                page,
                ProductToScrapDTO(
                    name = "açúcar",
                    search = "açúcar",
                    keyWords = emptyList(),
                    denyWords = listOf("cristal"),
                    quantityBase = QuantityBase.GRAMS,
                ),
            )
        } finally {
            page.close()
        }

        val saved = priceService.list(ListProductRequest()).data
        assertEquals(1, saved.size)
        assertEquals("açúcar", saved[0].name)
        assertEquals(1, saved[0].location)
        assertEquals(QuantityBase.GRAMS,saved[0].quantityBase )
        assertEquals(3790,saved[0].price)
        assertEquals(saved[0].brand, "acucar refinado uniao 1kg" )
    }
}
