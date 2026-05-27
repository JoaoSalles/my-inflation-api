package com.salles.domain

import kotlinx.serialization.Serializable

@Serializable
enum class QuantityBase {
    GRAMS,
    UNITS,
    MILLILITERS,
}