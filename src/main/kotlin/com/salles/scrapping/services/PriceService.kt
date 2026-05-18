package com.salles.scrapping.services

import com.salles.scrapping.data.ListProductPriceRequest
import com.salles.scrapping.data.ListProductRequest
import com.salles.scrapping.data.PagedResponse
import com.salles.scrapping.data.PriceDTO
import com.salles.scrapping.db.entities.PriceDailyAvgEntity
import com.salles.scrapping.db.entities.PriceEntity
import com.salles.scrapping.repositories.PriceRepository

class PriceService(
    private val repository: PriceRepository,
) {
    suspend fun create(newPrice: PriceDTO): PriceEntity = repository.create(
            productName  = newPrice.productName,
            brand        = newPrice.brand,
            price        = newPrice.price,
            quantityBase = newPrice.quantityBase,
            productLabel = newPrice.productLabel,
        )

    suspend fun list(request: ListProductRequest): PagedResponse<PriceEntity> {
        val (data, hasNext) = repository.list(request.product, request.from, request.to, request.page, request.pageSize)
        return PagedResponse(data, request.page, request.pageSize, hasNext)
    }
    suspend fun listProductPrice(request: ListProductPriceRequest): PagedResponse<PriceDailyAvgEntity> {
        val (data, hasNext) = repository.listProductPrice(request.product, request.from, request.to, request.page, request.pageSize)
        return PagedResponse(data, request.page, request.pageSize, hasNext)
    }
}