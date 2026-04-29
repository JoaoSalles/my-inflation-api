package com.salles.scrapping.db.entities

import com.salles.scrapping.domain.QuantityBase
import kotlinx.serialization.Serializable

@Serializable
data class ProductToScrapEntity(
    val id: Long,
    val productName: String,
    val quantityBase: QuantityBase,
    val keyWords: List<String>,
)
