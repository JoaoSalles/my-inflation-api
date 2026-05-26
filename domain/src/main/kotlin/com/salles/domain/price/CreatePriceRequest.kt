package com.salles.domain.price

import com.salles.domain.QuantityBase

interface CreatePriceRequest {
    val name: String
    val brand: String
    val price: Int
    val quantityBase: QuantityBase
    val location: Int
    val productLabel: String?
}
