package com.salles.scrapping.data

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ListPriceRequest(
    val from: Instant? = null,
    val to: Instant? = null,
    val page: Int? = null,
    val pageSize: Int? = null,
)