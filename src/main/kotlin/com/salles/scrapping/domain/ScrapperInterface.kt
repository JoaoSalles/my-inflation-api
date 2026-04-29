package com.salles.scrapping.domain

import com.salles.scrapping.db.entities.ProductToScrapEntity


interface Scrapper<T> {
    suspend fun scrap(product: ProductToScrapEntity): List<T>
    suspend fun parseProducts(productToScrap: ProductToScrap, products: List<SearchResponse>): List<SearchResponse>
}