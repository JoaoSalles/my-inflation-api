package com.salles.scrapping.domain.scrapper

import com.salles.scrapping.domain.SearchResponse

interface Scrapper<T, G> {
    suspend fun scrap(product: G): List<T>
    suspend fun parseProducts(productToScrap: G, products: List<SearchResponse>): List<SearchResponse>
}