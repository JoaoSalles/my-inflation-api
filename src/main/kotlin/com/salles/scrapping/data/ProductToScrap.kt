package com.salles.scrapping.data

import com.salles.scrapping.domain.ProductToScrap
import com.salles.scrapping.domain.QuantityBase

data class ProductToScrap(
    override val name: String,
    override val keyWords: List<String>,
    override val quantityBase: QuantityBase,
) : ProductToScrap
