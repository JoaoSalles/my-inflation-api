package com.salles.scrapping.services

import com.salles.scrapping.data.CreateProductToScrapRequest
import com.salles.scrapping.data.PriceDTO
import com.salles.scrapping.db.entities.PriceEntity
import com.salles.scrapping.repositories.PriceRepository
import kotlin.time.Instant

class PriceService(
    private val repository: PriceRepository,
) {
    suspend fun create(newPrice: PriceDTO): PriceEntity = repository.create(
            newPrice.productName,
            newPrice.brand,
            newPrice.price,
            newPrice.quantityBase,
        )

    suspend fun list(
        from: Instant?,
        to: Instant?,
        page: Int,
        pageSize: Int
    ): List<PriceEntity> = repository.list(
        from,
        to,
        page,
        pageSize
    )
}