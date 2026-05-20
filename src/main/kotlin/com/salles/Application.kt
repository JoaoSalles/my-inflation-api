package com.salles

import com.salles.scrapping.db.PostgresDatabaseFactory
import com.salles.scrapping.repositories.PostgresPriceRepository
import com.salles.scrapping.repositories.PostgresProductToScrapRepository
import com.salles.scrapping.repositories.ProductToScrapRepository
import com.salles.scrapping.routes.productToScrapRoutes
import com.salles.scrapping.routes.scrappingRoutes
import com.salles.scrapping.services.ProductToScrapService
import com.salles.scrapping.repositories.PriceRepository
import com.salles.scrapping.routes.priceRoutes
import com.salles.scrapping.services.PriceService

import com.salles.scrapping.plugins.CloudflareValidation
import com.salles.scrapping.services.ScrappingService
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    System.getenv("CLOUDFLARE_SECRET")?.let { secret ->
        install(CloudflareValidation) {
            this.secret = secret
        }
    }

    install(Koin) {
        modules(module {
            single<HttpClient> {
                HttpClient(OkHttp) {
                    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                        json()
                    }
                    install(Logging) {
                        logger = Logger.DEFAULT
                        level = LogLevel.INFO
                    }
                }
            }
            single { ScrappingService(get(), get()) }
            single<ProductToScrapRepository> { PostgresProductToScrapRepository() }
            single { ProductToScrapService(get()) }
            single<PriceRepository> { PostgresPriceRepository() }
            single { PriceService(get()) }
        })
    }

    val db = PostgresDatabaseFactory(environment.config)
    monitor.subscribe(ApplicationStopped) { db.close() }

    scrappingRoutes()
    productToScrapRoutes()
    priceRoutes()
}
