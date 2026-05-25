package com.salles.scrapping.domain.repositories

import com.salles.scrapping.data.price.PriceDTO
import com.salles.scrapping.domain.QuantityBase
import com.salles.scrapping.domain.price.PriceInterface
import com.salles.scrapping.domain.price.PriceAvgInterface
import kotlin.time.Instant

interface PrinceRepositoryInterface {
    suspend fun create(
        productName: String,
        brand: String,
        price: Int,
        quantityBase: QuantityBase,
        location: Int = 0,
        productLabel: String? = null,
    ): PriceInterface

    suspend fun list(
        product: String?,
        from: Instant? = null,
        to: Instant? = null,
        page: Int = 0,
        pageSize: Int = 20,
    ): Pair<List<PriceDTO>, Boolean>

    suspend fun listProductPrice(
        product: String,
        from: Instant? = null,
        to: Instant? = null,
        page: Int = 0,
        pageSize: Int = 20,
    ): Pair<List<PriceAvgInterface>, Boolean>
}