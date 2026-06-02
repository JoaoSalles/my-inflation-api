package com.salles.root

import com.salles.api.repositories.PostgresPriceRepository
import com.salles.api.services.PriceService
import com.salles.data.PostgresDatabaseFactory
import com.salles.scrapper.repositories.PostgresProductToScrapRepository
import com.salles.scrapper.services.ProductToScrapService
import com.salles.scrapper.services.ScrappingService
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("ScraperJob")

/**
 * Standalone batch entry point for the daily scrape. Boots only the dependencies the scrape
 * needs (no web server), runs every product to completion, then exits — exit code 0 on success
 * and 1 on failure so a scheduler can detect a failed run. Mirrors the wiring in
 * [com.salles.root.module].
 */
fun main() {
    val startedAt = System.currentTimeMillis()
    val db = PostgresDatabaseFactory.fromEnv()
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json() }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }

    var exitCode = 0
    try {
        runBlocking {
            val priceService = PriceService(PostgresPriceRepository())
            val products = ProductToScrapService(PostgresProductToScrapRepository()).list().data
            log.info("Starting scrape of {} products", products.size)
            ScrappingService(client, priceService).runScrapping(products)
        }
        log.info("Scrape finished in {}ms", System.currentTimeMillis() - startedAt)
    } catch (e: Throwable) {
        log.error("Scrape job failed", e)
        exitCode = 1
    } finally {
        client.close()
        db.close()
    }

    // Force exit: Playwright/OkHttp can leave non-daemon threads that would otherwise keep
    // the JVM (and the scheduled machine) alive past the work.
    exitProcess(exitCode)
}
