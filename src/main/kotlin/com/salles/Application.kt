package com.salles

import com.salles.scrapping.routes.scrappingRoutes
import com.salles.scrapping.services.ScrappingService
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
    install(Koin) {
        modules(module {
            single { ScrappingService() }
        })
    }

    scrappingRoutes()
}
