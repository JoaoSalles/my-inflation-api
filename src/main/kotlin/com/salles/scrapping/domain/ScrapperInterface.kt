package com.salles.scrapping.domain


interface Scrapper<T> {
    suspend fun scrap(product: String): List<T>
    suspend fun parseProducts(productToScrap: ProductToScrap, products: List<SearchResponse>): List<SearchResponse>
}