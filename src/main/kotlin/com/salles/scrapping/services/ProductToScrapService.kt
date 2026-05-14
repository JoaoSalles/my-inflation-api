package com.salles.scrapping.services

import com.salles.scrapping.db.entities.ProductToScrapEntity
import com.salles.scrapping.repositories.ProductToScrapRepository
import com.salles.scrapping.data.CreateProductToScrapRequest
import com.salles.scrapping.data.PagedResponse
import com.salles.scrapping.data.ScrapRequest

class ProductToScrapService(
    private val repository: ProductToScrapRepository,
) {
    suspend fun create(request: CreateProductToScrapRequest): ProductToScrapEntity =
        repository.create(
            productName  = request.productName,
            search       = request.search,
            quantityBase = request.quantityBase,
            keyWords     = request.keyWords,
            denyWords    = request.denyWords,
        )

    suspend fun list(request: ScrapRequest = ScrapRequest()): PagedResponse<ProductToScrapEntity> {
        val (data, hasNext) = repository.list(request.product)
        return PagedResponse(data, 0, 0, hasNext)
    }

    suspend fun listDistinct(): PagedResponse<ProductToScrapEntity> {
        val (data, hasNext) = repository.listDistinct()
        return PagedResponse(data, 0, 0, hasNext)
    }
}
