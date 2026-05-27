package com.salles.domain.repositories

import com.salles.domain.QuantityBase
import com.salles.domain.price.PriceAvgInterface
import com.salles.domain.price.PriceInterface
import kotlin.time.Instant

interface PriceRepositoryInterface {
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
    ): Pair<List<PriceInterface>, Boolean>

    suspend fun listProductPrice(
        product: String,
        from: Instant? = null,
        to: Instant? = null,
        page: Int = 0,
        pageSize: Int = 20,
    ): Pair<List<PriceAvgInterface>, Boolean>
}
