package com.salles.scrapping.domain.scrapper

import com.salles.scrapping.domain.SearchResponse

interface PASearchResponseInterface : SearchResponse {
    val brand: String
    val unitPriceHomogeneousKit: Double?
}