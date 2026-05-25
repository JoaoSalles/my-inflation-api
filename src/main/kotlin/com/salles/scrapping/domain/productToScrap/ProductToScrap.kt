package com.salles.scrapping.domain.productToScrap

import com.salles.scrapping.domain.QuantityBase

interface ProductToScrap {
    val name: String
    val quantityBase: QuantityBase
}