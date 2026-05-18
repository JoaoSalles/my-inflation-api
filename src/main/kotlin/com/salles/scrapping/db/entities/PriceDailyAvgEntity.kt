package com.salles.scrapping.db.entities

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PriceDailyAvgEntity(
    val day: Instant,
    val product: String,
    val avgPrice: Int,
)
