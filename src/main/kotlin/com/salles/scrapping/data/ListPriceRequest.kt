package com.salles.scrapping.data

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ListProductPriceRequest(
    val product: String,
    val from: Instant? = null,
    val to: Instant? = null,
    val page: Int = 0,
    val pageSize: Int = 20,
)

@Serializable
data class ListProductRequest(
    val product: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val page: Int = 0,
    val pageSize: Int = 20,
)