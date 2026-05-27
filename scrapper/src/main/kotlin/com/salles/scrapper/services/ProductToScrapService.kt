package com.salles.scrapper.services

import com.salles.scrapper.data.PagedResponse
import com.salles.scrapper.data.productToScrap.ProductToScrapDTO
import com.salles.scrapper.data.scrap.ScrapRequest
import com.salles.scrapper.repositories.ProductToScrapRepository

class ProductToScrapService(
    private val repository: ProductToScrapRepository,
) {
    suspend fun list(request: ScrapRequest = ScrapRequest()): PagedResponse<ProductToScrapDTO> {
        val (data, hasNext) = repository.list(request.product)
        return PagedResponse(data, 0, 0, hasNext)
    }
}
