package com.salles.scrapper.scrapers

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.salles.domain.SearchResponse
import com.salles.domain.services.PriceServiceInterface
import com.salles.scrapper.data.price.CreatePriceCommand
import com.salles.scrapper.data.productToScrap.ProductToScrapDTO
import com.salles.scrapper.data.scrap.CaSearchResponse
import com.salles.scrapper.utils.containsDenyword
import com.salles.scrapper.utils.matchesKeywords
import com.salles.scrapper.utils.pricePerQuantity
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(CaScrapper::class.java)

class CaScrapper(
    private val priceService: PriceServiceInterface? = null,
    private val postalCode: String = "04546002",
) {

    /**
     * Launches a headless browser, loads the Carrefour search page for [product] (a VTEX/Remix
     * SPA, so we wait for the product grid to render), then extracts and persists the results.
     * The VTEX delivery region is seeded in localStorage before navigation so the rendered
     * prices reflect [postalCode]. Returns an empty list and logs on any failure.
     */
    suspend fun scrap(product: ProductToScrapDTO): List<CaSearchResponse> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(product.search, Charsets.UTF_8).replace("+", "%20")
        val url = "https://mercado.carrefour.com.br/busca/$encoded"
        try {
            Playwright.create().use { playwright ->
                playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(true)).use { browser ->
                    val page = browser.newPage()
                    // Seed VTEX delivery region before the SPA boots so prices reflect the postal code.
                    page.addInitScript(
                        """
                        try {
                          window.localStorage.setItem('vtex:postalCode', '$postalCode');
                          window.localStorage.setItem('vtex:deliveryType', 'residential');
                          window.localStorage.setItem('skipRegionalizator', 'true');
                        } catch (e) {}
                        """.trimIndent()
                    )
                    page.navigate(url)
                    page.waitForSelector("[data-id=\"product-grid\"]")
                    scrapFromPage(page, product)
                }
            }
        } catch (e: Exception) {
            // TODO create a flow for errors, to keep track of scrapping that failed
            log.error("Carrefour search error for term='${product.search}'", e)
            emptyList()
        }
    }

    /**
     * Extracts the cards rendered on [page], filters/computes prices via [parseProducts],
     * and persists one price row per parsed product (location 1 = Carrefour). Persistence
     * failures for individual rows are logged and do not abort the rest. Returns the parsed list.
     */
    suspend fun scrapFromPage(page: Page, product: ProductToScrapDTO): List<CaSearchResponse> {
        val parsed = parseProducts(product, extractCards(page))
        parsed.forEach { p ->
            try {
                priceService?.create(
                    CreatePriceCommand(
                        name = p.name,
                        brand = p.brand,
                        price = p.price ?: 0,
                        quantityBase = product.quantityBase,
                        location = 1,
                        productLabel = p.brand,
                    )
                )
            } catch (e: Exception) {
                // TODO create a flow for errors, to keep track of scrapping that failed
                log.error("Failed to save price for '${product.name}' - brand: ${p.brand}", e)
            }
        }
        println("parsed ${product.name}: ${parsed}")
        return parsed
    }

    /**
     * Reads every product card currently rendered on [page] and builds a [CaSearchResponse]
     * (price + title) for each. Cards missing a title or price element are skipped.
     */
    fun extractCards(page: Page): List<CaSearchResponse> =
        page.querySelectorAll("[data-testid=\"search-product-card\"]").mapNotNull { card ->
            val title = card.querySelector("h2")?.textContent()?.trim()
            val priceText = card.querySelector("span.text-price-default")?.textContent()?.trim()
            if (title.isNullOrEmpty() || priceText.isNullOrEmpty()) return@mapNotNull null
            CaSearchResponse(price = parsePrice(priceText), name = title)
        }

    fun parseProducts(
        productToScrap: ProductToScrapDTO,
        products: List<SearchResponse>,
    ): List<CaSearchResponse> {
        val result = mutableListOf<CaSearchResponse>()
        for (product in products) {
            if (product.name.isEmpty()) continue
            if (!matchesKeywords(product.name, productToScrap.keyWords) ||
                containsDenyword(product.name, productToScrap.denyWords, productToScrap.quantityBase)) continue

            val price = pricePerQuantity(productToScrap.quantityBase, product)
            if (price == 0) continue

            result.add(CaSearchResponse(price = price, name = product.name.take(100), brand = product.name.take(80)))
        }
        return result
    }

    /** "R$ 3,79" / "R$ 1.234,56" → centavos; 0 when no parseable number. */
    fun parsePrice(text: String): Int {
        val cleaned = text.replace("R$", "").trim().replace(".", "").replace(",", ".")
        return ((cleaned.toDoubleOrNull() ?: return 0) * 100).toInt()
    }
}
