package com.salles.scrapping.services

import com.salles.scrapping.repositories.ProductToScrapRepository
import com.salles.scrapping.data.CreateProductToScrapRequest
import com.salles.scrapping.data.PagedResponse
import com.salles.scrapping.data.ScrapRequest
import com.salles.scrapping.domain.ProductToScrap

class ProductToScrapService(
    private val repository: ProductToScrapRepository,
) {
    suspend fun create(request: CreateProductToScrapRequest): ProductToScrap =
        repository.create(
            productName  = request.productName,
            search       = request.search,
            quantityBase = request.quantityBase,
            keyWords     = request.keyWords,
            denyWords    = request.denyWords,
        )

    suspend fun list(request: ScrapRequest = ScrapRequest()): PagedResponse<ProductToScrap> {
        val (data, hasNext) = repository.list(request.product)
        return PagedResponse(data, 0, 0, hasNext)
    }

    suspend fun listDistinct(): PagedResponse<ProductToScrap> {
        val (data, hasNext) = repository.listDistinct()
        return PagedResponse(data, 0, 0, hasNext)
    }
}
