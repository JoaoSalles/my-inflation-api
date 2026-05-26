package com.salles.scrapper.data.price

import com.salles.domain.price.PriceAvgInterface
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class PriceAVGResponse (
    override val day: Instant,
    override val product: String,
    override val avgPrice: Int,
) : PriceAvgInterface
