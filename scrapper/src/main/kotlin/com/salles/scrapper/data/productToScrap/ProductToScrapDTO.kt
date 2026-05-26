package com.salles.scrapper.data.productToScrap

import com.salles.domain.QuantityBase
import com.salles.domain.productToScrap.ProductToScrap
import kotlinx.serialization.Serializable

@Serializable
data class ProductToScrapDTO(
    override val name: String,
    val search: String,
    val keyWords: List<String>,
    val denyWords: List<String>,
    override val quantityBase: QuantityBase,
) : ProductToScrap

@Serializable
data class CreateProductToScrapRequest(
    override val name: String,
    val search: String,
    override val quantityBase: QuantityBase,
    val keyWords: List<String> = emptyList(),
    val denyWords: List<String> = emptyList(),
) : ProductToScrap