package com.salles.database.entities

import com.salles.scrapping.domain.QuantityBase

data class ProductToScrapEntity(
    val id: Long,
    val productName: String,
    val quantityBase: QuantityBase,
    val keyWords: List<String>,
)
