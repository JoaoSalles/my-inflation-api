package com.salles.api.data.price

import com.salles.domain.QuantityBase
import com.salles.domain.price.PriceInterface
import kotlinx.serialization.Serializable

@Serializable
data class PriceDTO(
    override val name: String,
    override val brand: String,
    override val price: Int,
    override val quantityBase: QuantityBase,
    override val location: Int,
    override val productLabel: String? = null,
) : PriceInterface {


}
