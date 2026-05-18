package com.salles.scrapping.domain

interface ProductToScrap {
    val name: String
    val search: String?
    val keyWords: List<String>?
    val denyWords: List<String>?
    val quantityBase: QuantityBase
}