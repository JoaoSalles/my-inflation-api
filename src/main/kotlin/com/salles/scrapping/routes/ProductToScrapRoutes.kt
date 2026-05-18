package com.salles.scrapping.routes

import com.salles.scrapping.db.ProductNameAlreadyExistsException
import com.salles.scrapping.data.CreateProductToScrapRequest
import com.salles.scrapping.scrapers.PAScrapper
import com.salles.scrapping.services.ProductToScrapService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

fun Application.productToScrapRoutes() {
    val service: ProductToScrapService by inject()

    routing {
        route("/product") {
            post() {
                val request = call.receive<CreateProductToScrapRequest>()
                try {
                    val entity = service.create(request)
                    call.respond(HttpStatusCode.Created, entity)
                } catch (e: ProductNameAlreadyExistsException) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse(e.message ?: "Product already exists"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
                }
            }

            get() {
                try {
                    call.respond(HttpStatusCode.OK, service.listDistinct())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
                }
            }
        }
    }
}

@Serializable
private data class ErrorResponse(val error: String)