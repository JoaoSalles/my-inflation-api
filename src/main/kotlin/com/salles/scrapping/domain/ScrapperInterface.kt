package com.salles.scrapping.domain

import com.salles.scrapping.domain.ProductToScrap

interface Scrapper<T> {
    suspend fun scrap(product: ProductToScrap): List<T>
    suspend fun parseProducts(productToScrap: ProductToScrap, products: List<SearchResponse>): List<SearchResponse>
}