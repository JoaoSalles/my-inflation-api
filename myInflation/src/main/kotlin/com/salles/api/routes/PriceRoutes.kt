package com.salles.api.routes

import com.salles.api.data.PagedResponse
import com.salles.api.data.price.ListProductPriceRequest
import com.salles.api.data.price.ListProductRequest
import com.salles.api.data.price.PriceAVGResponse
import com.salles.api.data.price.PriceDTO
import com.salles.domain.services.PriceServiceInterface
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlin.time.Instant
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("PriceRoutes")

fun Application.priceRoutes() {
    val service: PriceServiceInterface by inject()

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
                    @Suppress("UNCHECKED_CAST")
                    call.respond(HttpStatusCode.OK, service.list(request) as PagedResponse<PriceDTO>)
                } catch (e: Exception) {
                    log.error("product-log error: ${e::class.simpleName} - ${e.message}", e)
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
                    @Suppress("UNCHECKED_CAST")
                    call.respond(HttpStatusCode.OK, service.listProductPrice(request) as PagedResponse<PriceAVGResponse>)
                } catch (e: Exception) {
                    log.error("products error: ${e::class.simpleName} - ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
                }
            }
        }
    }
}