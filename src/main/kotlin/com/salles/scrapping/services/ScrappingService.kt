package com.salles.scrapping.services

import com.salles.scrapping.db.entities.ProductToScrapEntity
import com.salles.scrapping.scrapers.PAScrapper
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ScrappingService(
    private val client: HttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val PAScrapper = PAScrapper(client);

    suspend fun launchScrapping(products: List<ProductToScrapEntity> = emptyList()) {
        products.forEach { product  ->
            delay(500.milliseconds)
            scope.launch {
                PAScrapper.scrap(product)
            }
        }
    }
}
