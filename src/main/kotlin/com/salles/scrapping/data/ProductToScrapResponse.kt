package com.salles.scrapping.data

import com.salles.scrapping.domain.ProductToScrap
import com.salles.scrapping.domain.QuantityBase
import kotlinx.serialization.Serializable

@Serializable
data class ProductToScrapResponse(
    val id: Long,
    val productName: String,
    override val search: String?,
    override val quantityBase: QuantityBase,
    override val keyWords: List<String>?,
    override val denyWords: List<String>?,
): ProductToScrap {
    override val name: String get() = productName;

}
