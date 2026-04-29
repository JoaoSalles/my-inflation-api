package com.salles.scrapping.routes

import com.salles.database.DatabaseException
import com.salles.database.ProductNameAlreadyExistsException
import com.salles.scrapping.data.CreateProductToScrapRequest
import com.salles.scrapping.services.ProductToScrapService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Application.productToScrapRoutes() {
    val service: ProductToScrapService by inject()

    routing {
        post("/products") {
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
    }
}

@Serializable
private data class ErrorResponse(val error: String)