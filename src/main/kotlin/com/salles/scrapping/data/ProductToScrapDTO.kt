package com.salles.scrapping.data

import com.salles.scrapping.domain.ProductToScrap
import com.salles.scrapping.domain.QuantityBase

data class ProductToScrapDTO(
    override val name: String,
    override val search: String,
    override val keyWords: List<String>,
    override val denyWords: List<String>,
    override val quantityBase: QuantityBase,
) : ProductToScrap
