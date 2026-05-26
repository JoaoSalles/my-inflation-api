package com.salles.scrapper.services

import com.salles.scrapper.data.PagedResponse
import com.salles.domain.price.CreatePriceRequest
import com.salles.domain.price.ListProductPriceRequestInterface
import com.salles.domain.price.ListProductRequestInterface
import com.salles.domain.price.PriceInterface
import com.salles.domain.price.PriceAvgInterface
import com.salles.domain.repositories.PriceRepositoryInterface
import com.salles.domain.services.PriceServiceInterface

class PriceService(
    private val repository: PriceRepositoryInterface,
): PriceServiceInterface {
    override suspend fun create(request: CreatePriceRequest): PriceInterface = repository.create(
        productName  = request.name,
        brand        = request.brand,
        price        = request.price,
        quantityBase = request.quantityBase,
        location     = request.location,
        productLabel = request.productLabel,
    )

    override suspend fun list(request: ListProductRequestInterface): PagedResponse<PriceInterface> {
        val (data, hasNext) = repository.list(request.product, request.from, request.to, request.page, request.pageSize)
        return PagedResponse(data, request.page, request.pageSize, hasNext)
    }

    override suspend fun listProductPrice(request: ListProductPriceRequestInterface): PagedResponse<PriceAvgInterface> {
        val (data, hasNext) = repository.listProductPrice(request.product, request.from, request.to, request.page, request.pageSize)
        return PagedResponse(data, request.page, request.pageSize, hasNext)
    }
}