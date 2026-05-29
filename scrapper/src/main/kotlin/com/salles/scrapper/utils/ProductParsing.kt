package com.salles.scrapper.utils

import com.salles.domain.QuantityBase
import com.salles.domain.SearchResponse
import java.text.Normalizer

/** Strips diacritics and lowercases so matching is accent- and case-insensitive. */
fun normalize(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}"), "")
        .lowercase()

/** All keyWords (accent- and case-insensitive) must appear in [name]. Empty keyWords ⇒ matches. */
fun matchesKeywords(name: String, keyWords: List<String>): Boolean {
    val normalizedName = normalize(name)
    return keyWords.isEmpty() || keyWords.all { normalizedName.contains(normalize(it)) }
}

/** Denywords always applied, on top of the caller-supplied ones. */
val defaultDenyWords = listOf("+", "kit", "pack", "combo")

/**
 * True if any denyWord (accent- and case-insensitive) appears in [name]. Empty denyWords ⇒ false.
 * Multi-item packs ("N unidades") are denied for every [quantityBase] except [QuantityBase.UNITS],
 * where a unit count is the expected, legitimate quantity.
 */
fun containsDenyword(name: String, denyWords: List<String>, quantityBase: QuantityBase): Boolean {
    val normalizedName = normalize(name)
    val effectiveDenyWords = denyWords + defaultDenyWords +
        if (quantityBase != QuantityBase.UNITS) listOf("unidades") else emptyList()
    return effectiveDenyWords.any { normalizedName.contains(normalize(it)) }
}

/** Dispatches to the matching per-quantity helper. */
fun pricePerQuantity(quantityBase: QuantityBase, product: SearchResponse): Int =
    when (quantityBase) {
        QuantityBase.GRAMS -> parseProductsPerGram(product)
        QuantityBase.UNITS -> parseProductsPerUnits(product)
        QuantityBase.MILLILITERS -> parseProductsPerMilliliters(product)
    }

/*
* There is a problem using grams and milliliter, it may cost less than a cent
* so the integer value will be a representation of original value divided by 10000
* */

fun parseProductsPerGram(product: SearchResponse): Int {
    val name = product.name
    val kgRegex = Regex("""(\d+(?:[.,]\d+)?)\s*kg\b""")
    val gRegex = Regex("""(\d+(?:[.,]\d+)?)\s*g\b""")

    val grams: Double = kgRegex.find(name)?.groupValues?.get(1)
        ?.replace(',', '.')
        ?.toDouble()
        ?.times(1000)
        ?: gRegex.find(name)?.groupValues?.get(1)
            ?.replace(',', '.')
            ?.toDouble()
        ?: return 0

    return normalizeForMillicent((product.price ?: 0) / grams)
}

fun parseProductsPerUnits(product: SearchResponse): Int {
    val unidadeRegex = Regex("""(\d+)\s*[Uu]nidades?""")
    val units = unidadeRegex.find(product.name)?.groupValues?.get(1)?.toIntOrNull() ?: return 1
    return ((product.price ?: 0) / units) * 10
}

fun parseProductsPerMilliliters(product: SearchResponse): Int {
    val name = product.name
    val mlRegex = Regex("""(\d+(?:[.,]\d+)?)\s*ml\b""", RegexOption.IGNORE_CASE)
    val lRegex = Regex("""(\d+(?:[.,]\d+)?)\s*l\b""", RegexOption.IGNORE_CASE)

    val milliliters: Double = lRegex.find(name)?.groupValues?.get(1)
        ?.replace(',', '.')
        ?.toDouble()
        ?.times(1000)
        ?: mlRegex.find(name)?.groupValues?.get(1)
            ?.replace(',', '.')
            ?.toDouble()
        ?: return 0

    return normalizeForMillicent((product.price ?: 0) / milliliters)
}
