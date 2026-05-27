package com.salles.scrapper.data.price

import com.salles.domain.price.ListProductPriceRequestInterface
import com.salles.domain.price.ListProductRequestInterface
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ListProductPriceRequest(
    override val product: String,
    override val from: Instant? = null,
    override val to: Instant? = null,
    override val page: Int = 0,
    override val pageSize: Int = 20,
) : ListProductPriceRequestInterface

@Serializable
data class ListProductRequest(
    override val product: String? = null,
    override val from: Instant? = null,
    override val to: Instant? = null,
    override val page: Int = 0,
    override val pageSize: Int = 20,
) : ListProductRequestInterface