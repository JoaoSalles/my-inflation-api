package com.salles.scrapping.data

import com.salles.scrapping.domain.QuantityBase
import kotlinx.serialization.Serializable

@Serializable
data class CreateProductToScrapRequest(
    val productName: String,
    val quantityBase: QuantityBase,
    val keyWords: List<String>,
)
