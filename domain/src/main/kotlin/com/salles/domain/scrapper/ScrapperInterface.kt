package com.salles.domain.scrapper

import com.salles.domain.SearchResponse

interface Scrapper<T, G> {
    suspend fun scrap(product: G): List<T>
    suspend fun parseProducts(productToScrap: G, products: List<SearchResponse>): List<SearchResponse>
}
