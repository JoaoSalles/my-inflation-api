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
import kotlin.collections.forEach
import kotlin.time.Duration.Companion.milliseconds

class ScrappingService(
    private val client: HttpClient,
    private val priceService: PriceServiceInterface,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scrapperPA = PAScrapper(client, priceService)
    private val scrapperCa = CaScrapper(priceService)

    fun launchScrapping(products: List<ProductToScrapDTO> = emptyList()) {
        scope.launch {
            products.forEach { product ->
                delay(500.milliseconds)
                launch { scrapperPA.scrap(product) }
                launch { scrapperCa.scrap(product) }
            }
        }
    }
}
