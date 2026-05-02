package com.salles.scrapping.db.entities

import com.salles.scrapping.domain.QuantityBase
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PriceEntity(
    val time: Instant,
    val location: Int,
    val product: String,
    val price: Int,
    val brand: String,
    val quantityBase: QuantityBase,
    val createdAt: Instant,
)