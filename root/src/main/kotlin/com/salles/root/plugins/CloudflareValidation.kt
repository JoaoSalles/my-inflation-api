package com.salles.root.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.header
import io.ktor.server.response.respond

class CloudflareValidationConfig {
    var secret: String = ""
    var headerName: String = "Origin-Token"
}

val CloudflareValidation = createApplicationPlugin(
    name = "CloudflareValidation",
    createConfiguration = ::CloudflareValidationConfig,
) {
    val secret = pluginConfig.secret
    val headerName = pluginConfig.headerName

    onCall { call ->
        val incoming = call.request.header(headerName)
        if (incoming == null || incoming != secret) {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}
