package com.salles.domain.price

import com.salles.domain.QuantityBase

interface PriceInterface {
    val location: Int
    val name: String
    val price: Int
    val brand: String
    val quantityBase: QuantityBase
    val productLabel: String?
}
