package com.salles.scrapping.services

import com.salles.scrapping.repositories.ProductToScrapRepository
import com.salles.scrapping.data.productToScrap.CreateProductToScrapRequest
import com.salles.scrapping.data.PagedResponse
import com.salles.scrapping.data.productToScrap.ProductToScrapDTO
import com.salles.scrapping.data.scrap.ScrapRequest
import com.salles.scrapping.data.productToScrap.ProductToScrapCreateResponse

class ProductToScrapService(
    private val repository: ProductToScrapRepository,
) {
    suspend fun create(request: CreateProductToScrapRequest): ProductToScrapCreateResponse =
        repository.create(
            productName  = request.name,
            search       = request.search,
            quantityBase = request.quantityBase,
            keyWords     = request.keyWords,
            denyWords    = request.denyWords,
        )

    suspend fun list(request: ScrapRequest = ScrapRequest()): PagedResponse<ProductToScrapDTO> {
        val (data, hasNext) = repository.list(request.product)
        return PagedResponse(data, 0, 0, hasNext)
    }

    suspend fun listDistinct(): PagedResponse<ProductToScrapCreateResponse> {
        val (data, hasNext) = repository.listDistinct()
        return PagedResponse(data, 0, 0, hasNext)
    }
}
