package com.salles.scrapping.data

import com.salles.scrapping.domain.QuantityBase
import kotlinx.serialization.Serializable

@Serializable
data class PriceDTO(
    val productName: String,
    val brand: String,
    val price: Int,
    val quantityBase: QuantityBase,
    val location: Int,
    val productLabel: String? = null,
)