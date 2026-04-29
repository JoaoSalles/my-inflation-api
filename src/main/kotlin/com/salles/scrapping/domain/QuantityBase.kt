package com.salles.scrapping.domain

import kotlinx.serialization.Serializable

@Serializable
enum class QuantityBase {
    GRAMS,
    UNITS,
    MILLILITERS,
}