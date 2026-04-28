package com.salles.scrapping.services

import com.salles.database.entities.ProductToScrapEntity
import com.salles.database.repositories.ProductToScrapRepository
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
