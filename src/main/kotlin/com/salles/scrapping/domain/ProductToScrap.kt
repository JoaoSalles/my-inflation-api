package com.salles.scrapping.domain

interface ProductToScrap {
    val name: String
    val keyWords: List<String>
    val denyWords: List<String>
    val quantityBase: QuantityBase
}