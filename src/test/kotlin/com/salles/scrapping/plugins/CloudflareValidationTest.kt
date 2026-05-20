package com.salles.scrapping.plugins

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.install
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class CloudflareValidationTest {

    private fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                install(CloudflareValidation) {
                    secret = "test-secret"
                    headerName = "X-CF-Origin-Token"
                }
                routing { get("/") { call.respond(HttpStatusCode.OK) } }
            }
            block()
        }

    @Test
    fun `missing header returns 401`() = testApp {
        val response = client.get("/")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `wrong header value returns 401`() = testApp {
        val response = client.get("/") {
            header("X-CF-Origin-Token", "wrong-secret")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `correct header value returns 200`() = testApp {
        val response = client.get("/") {
            header("X-CF-Origin-Token", "test-secret")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
