package com.salles.scrapper.routes

import com.salles.scrapper.data.scrap.ScrapRequest
import com.salles.scrapper.services.ProductToScrapService
import com.salles.scrapper.services.ScrappingService
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
//import org.slf4j.LoggerFactory

//private val logger = LoggerFactory.getLogger("ScrappingRoutes")

fun Application.scrappingRoutes() {
    val service: ScrappingService by inject()
    val productToScrapService: ProductToScrapService by inject()

    routing {
        get("/scrapping") {
            val request = ScrapRequest(product = call.parameters["product"])
            val list = productToScrapService.list(request).data
            service.launchScrapping(list)
            call.respond(HttpStatusCode.Accepted)
        }
    }
}
