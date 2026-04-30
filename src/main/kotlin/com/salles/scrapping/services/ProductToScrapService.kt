package com.salles.scrapping.services

import com.salles.scrapping.db.entities.ProductToScrapEntity
import com.salles.scrapping.repositories.ProductToScrapRepository
import com.salles.scrapping.data.CreateProductToScrapRequest

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

    suspend fun list(): List<ProductToScrapEntity> = repository.list()
}
