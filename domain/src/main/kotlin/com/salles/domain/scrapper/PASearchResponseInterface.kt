package com.salles.domain.scrapper

import com.salles.domain.SearchResponse

interface PASearchResponseInterface : SearchResponse {
    val brand: String
    val unitPriceHomogeneousKit: Double?
}
