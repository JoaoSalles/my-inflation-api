package com.salles.domain.services

import com.salles.domain.PagedResponseInterface
import com.salles.domain.price.CreatePriceRequest
import com.salles.domain.price.ListProductPriceRequestInterface
import com.salles.domain.price.ListProductRequestInterface
import com.salles.domain.price.PriceAvgInterface
import com.salles.domain.price.PriceInterface

interface PriceServiceInterface {
    suspend fun create(request: CreatePriceRequest): PriceInterface

    suspend fun list(request: ListProductRequestInterface): PagedResponseInterface<PriceInterface>

    suspend fun listProductPrice(request: ListProductPriceRequestInterface): PagedResponseInterface<PriceAvgInterface>
}
