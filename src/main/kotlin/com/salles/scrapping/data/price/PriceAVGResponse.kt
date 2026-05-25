package com.salles.scrapping.data.price

import com.salles.scrapping.domain.price.PriceAvgInterface
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class PriceAVGResponse (
    override val day: Instant,
    override val product: String,
    override val avgPrice: Int,
) : PriceAvgInterface
