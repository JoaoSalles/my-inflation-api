package com.salles.scrapping.routes

import com.salles.scrapping.data.ListProductPriceRequest
import com.salles.scrapping.data.ListProductRequest
import com.salles.scrapping.services.PriceService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlin.time.Instant
import org.koin.ktor.ext.inject

fun Application.priceRoutes() {
    val service: PriceService by inject()

    routing {
        route("/price") {
            get("/product-log") {
                try {
                    val request = ListProductRequest(
                        product  = call.request.queryParameters["product"]?.take(100),
                        from     = call.request.queryParameters["from"]?.let { Instant.parse(it) },
                        to       = call.request.queryParameters["to"]?.let { Instant.parse(it) },
                        page     = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                        pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(0, 100) ?: 20,
                    )
                    call.respond(HttpStatusCode.OK, service.list(request))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
                }
            }
            get("/products") {
                try {
                    val product = call.request.queryParameters["product"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "product is required"))

                    val request = ListProductPriceRequest(
                        product  = product.take(100),
                        from     = call.request.queryParameters["from"]?.let { Instant.parse(it) },
                        to       = call.request.queryParameters["to"]?.let { Instant.parse(it) },
                        page     = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                        pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 0,
                    )
                    call.respond(HttpStatusCode.OK, service.listProductPrice(request))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
                }
            }
        }
    }
}