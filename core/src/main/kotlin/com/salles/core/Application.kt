package com.salles.core

import com.salles.api.repositories.PostgresPriceRepository
import com.salles.api.repositories.PostgresProductToScrapRepository
import com.salles.api.repositories.ProductToScrapRepository
import com.salles.api.routes.productToScrapRoutes
import com.salles.api.routes.priceRoutes
import com.salles.api.services.PriceService
import com.salles.domain.services.PriceServiceInterface
import com.salles.api.services.ProductToScrapService
import com.salles.core.plugins.CloudflareValidation
import com.salles.data.PostgresDatabaseFactory
import com.salles.domain.repositories.PriceRepositoryInterface
import com.salles.scrapper.repositories.PostgresProductToScrapRepository as ScrapperProductToScrapRepository
import com.salles.scrapper.repositories.ProductToScrapRepository as ScrapperProductToScrapRepositoryInterface
import com.salles.scrapper.routes.scrappingRoutes
import com.salles.scrapper.services.ProductToScrapService as ScrapperProductToScrapService
import com.salles.scrapper.services.ScrappingService
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.CORS
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

    install(CORS) {
        allowHost("meuibge.com.br", schemes = listOf("https"))
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeaders { true }
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
            single<ScrapperProductToScrapRepositoryInterface> { ScrapperProductToScrapRepository() }
            single { ScrapperProductToScrapService(get()) }
            single<PriceRepositoryInterface> { PostgresPriceRepository() }
            single<PriceServiceInterface> { PriceService(get()) }
            single<ProductToScrapRepository> { PostgresProductToScrapRepository() }
            single { ProductToScrapService(get()) }
        })
    }

    val db = PostgresDatabaseFactory(environment.config)
    monitor.subscribe(ApplicationStopped) { db.close() }

    scrappingRoutes()
    productToScrapRoutes()
    priceRoutes()
}
