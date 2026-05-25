package com.salles.scrapping.data.productToScrap

import com.salles.scrapping.domain.productToScrap.ProductToScrap
import com.salles.scrapping.domain.QuantityBase
import kotlinx.serialization.Serializable

@Serializable
data class ProductToScrapCreateResponse(
    override val name: String,
    override val quantityBase: QuantityBase,
) : ProductToScrap
