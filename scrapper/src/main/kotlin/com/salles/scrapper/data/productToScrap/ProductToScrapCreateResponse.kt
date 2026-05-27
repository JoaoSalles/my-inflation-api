package com.salles.scrapper.data.productToScrap

import com.salles.domain.productToScrap.ProductToScrap
import com.salles.domain.QuantityBase
import kotlinx.serialization.Serializable

@Serializable
data class ProductToScrapCreateResponse(
    override val name: String,
    override val quantityBase: QuantityBase,
) : ProductToScrap
