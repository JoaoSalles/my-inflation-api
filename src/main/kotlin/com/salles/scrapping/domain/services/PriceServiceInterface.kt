package com.salles.scrapping.domain.services;

import com.salles.scrapping.domain.PagedResponseInterface;
import com.salles.scrapping.domain.price.ListProductPriceRequestInterface;
import com.salles.scrapping.domain.price.ListProductRequestInterface;
import com.salles.scrapping.domain.price.PriceInterface;
import com.salles.scrapping.domain.price.PriceAvgInterface;
import com.salles.scrapping.domain.price.CreatePriceRequest;

interface PriceServiceInterface {
    suspend fun create(request: CreatePriceRequest): PriceInterface

    suspend fun list(request: ListProductRequestInterface): PagedResponseInterface<PriceInterface>

    suspend fun listProductPrice(request:ListProductPriceRequestInterface): PagedResponseInterface<PriceAvgInterface>
}
