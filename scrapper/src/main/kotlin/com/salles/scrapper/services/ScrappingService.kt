package com.salles.scrapper.services

import com.salles.domain.services.PriceServiceInterface
import com.salles.scrapper.data.productToScrap.ProductToScrapDTO
import com.salles.scrapper.scrapers.CaScrapper
import com.salles.scrapper.scrapers.PAScrapper
import io.ktor.client.HttpClient

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.slf4j.LoggerFactory
import kotlin.collections.forEach
import kotlin.time.Duration.Companion.milliseconds

class ScrappingService(
    private val client: HttpClient,
    private val priceService: PriceServiceInterface,
) {
    private val log = LoggerFactory.getLogger(ScrappingService::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scrapperPA = PAScrapper(client, priceService)
    private val scrapperCa = CaScrapper(priceService)

    /** Fire-and-forget: returns immediately while scraping continues on a background scope. */
    fun launchScrapping(products: List<ProductToScrapDTO> = emptyList()) {
        scope.launch { runScrapping(products) }
    }

    /** Runs the scrape to completion; suspends until every product has been scraped. */
    suspend fun runScrapping(products: List<ProductToScrapDTO> = emptyList()) = supervisorScope {
        products.forEach { product ->
            delay(500.milliseconds)
            launch {
                try {
                    scrapperPA.scrap(product)
                } catch (e: Throwable) {
                    log.error("PA scrape failed for product '{}'", product.name, e)
                }
            }
            launch {
                try {
                    scrapperCa.scrap(product)
                } catch (e: Throwable) {
                    log.error("Ca scrape failed for product '{}'", product.name, e)
                }
            }
        }
    }
}
