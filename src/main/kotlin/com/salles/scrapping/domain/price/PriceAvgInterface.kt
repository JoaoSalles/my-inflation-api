package com.salles.scrapping.domain.price

import kotlin.time.Instant

interface PriceAvgInterface {
    val day: Instant
    val product: String
    val avgPrice: Int
}
