package com.salles.scrapping.routes

import com.salles.scrapping.services.ProductToScrapService
import com.salles.scrapping.services.ScrappingService
import io.ktor.client.HttpClient
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.scrappingRoutes() {
    val service: ScrappingService by inject()
    val productToScrapService: ProductToScrapService by inject()

    routing {
        get("/scrapping") {
            val list = productToScrapService.list();
            service.launchScrapping(list)
            call.respond(HttpStatusCode.Accepted)
        }
    }
}
