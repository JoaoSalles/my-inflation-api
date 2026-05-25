package com.salles.scrapping.domain.price

import kotlin.time.Instant

interface ListProductPriceRequestInterface {
    val product: String
    val from: Instant?
    val to: Instant?
    val page: Int
    val pageSize: Int
}