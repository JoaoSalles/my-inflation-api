package com.salles.scrapping.domain.price

import com.salles.scrapping.domain.QuantityBase

interface PriceInterface {
    val location: Int
    val name: String
    val price: Int
    val brand: String
    val quantityBase: QuantityBase
    val productLabel: String?
}


