package com.salles.api.data.productToScrap

import com.salles.domain.QuantityBase
import com.salles.domain.productToScrap.ProductToScrap
import kotlinx.serialization.Serializable

@Serializable
data class ProductToScrapResponse(
    val id: Long,
    override val name: String,
    val search: String?,
    override val quantityBase: QuantityBase,
    val keyWords: List<String>?,
    val denyWords: List<String>?,
): ProductToScrap {

}