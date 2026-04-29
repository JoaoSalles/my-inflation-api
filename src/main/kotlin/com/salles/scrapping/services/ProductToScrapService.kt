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
            quantityBase = request.quantityBase,
            keyWords     = request.keyWords,
        )

    suspend fun list(): List<ProductToScrapEntity> = repository.list()
}
