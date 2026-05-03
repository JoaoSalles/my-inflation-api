package com.salles.scrapping.routes

import com.salles.scrapping.services.PriceService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlin.time.Instant
import org.koin.ktor.ext.inject

fun Application.priceRoutes() {
    val service: PriceService by inject()

    routing {
        get("/prices") {
            try {
                val from     = call.request.queryParameters["from"]?.let { Instant.parse(it) }
                val to       = call.request.queryParameters["to"]?.let { Instant.parse(it) }
                val page     = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                call.respond(HttpStatusCode.OK, service.list(from, to, page, pageSize))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
            }
        }
    }
}