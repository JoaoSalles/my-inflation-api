package com.salles.scrapper.data.price

import com.salles.domain.QuantityBase
import com.salles.domain.price.CreatePriceRequest

data class CreatePriceCommand(
    override val name: String,
    override val brand: String,
    override val price: Int,
    override val quantityBase: QuantityBase,
    override val location: Int,
    override val productLabel: String? = null,
) : CreatePriceRequest