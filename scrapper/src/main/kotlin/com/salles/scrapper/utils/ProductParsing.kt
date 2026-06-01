package com.salles.scrapper.utils

import com.salles.domain.QuantityBase
import com.salles.domain.SearchResponse
import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

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

/**
 * Removes price outliers, picking the most reliable rule for the sample size and keeping only
 * products whose price lies within the resulting fences. Both absurdly high and absurdly low
 * values (typically parsing errors) are dropped; unpriced products are always dropped.
 *
 *  - `>= MIN_SAMPLE_FOR_IQR` priced products: Tukey's IQR rule, `[Q1 - kIqr*IQR, Q3 + kIqr*IQR]`.
 *  - `>= MIN_SAMPLE_FOR_MAD` (but fewer): median ± `kMad`*MAD, which still flags a lone outlier
 *    where IQR's quartiles would be too unstable to trust.
 *  - smaller still: nothing reliable to compare against, so every priced product is kept.
 *
 * Both rules use robust statistics (quartiles / median) that the outliers being removed do not move,
 * unlike a mean-based cutoff which the outliers themselves inflate.
 *
 * [kIqr] is Tukey's fence multiplier: 1.5 is standard ("mild" outliers); 3.0 flags only extreme ones.
 * [kMad] is the MAD multiplier: 3.0 ≈ "3 sigma" but immune to contamination.
 */
fun <T : SearchResponse> removePriceOutliers(
    products: MutableList<T>,
    kIqr: Double = 1.5,
    kMad: Double = 3.0,
): MutableList<T> {
    val prices = products.mapNotNull { it.price?.toDouble() }.sorted()
    val fences = when {
        prices.size >= MIN_SAMPLE_FOR_IQR -> iqrFences(prices, kIqr)
        prices.size >= MIN_SAMPLE_FOR_MAD -> madFences(prices, kMad)
        else -> null
    }
    return if (fences == null) {
        products.filter { it.price != null }.toMutableList()
    } else {
        products.filter { it.price?.let { price -> price.toDouble() in fences } ?: false }.toMutableList()
    }
}

private const val MIN_SAMPLE_FOR_IQR = 4
private const val MIN_SAMPLE_FOR_MAD = 3

/** Tukey fences `[Q1 - k*IQR, Q3 + k*IQR]` over a pre-sorted, non-empty price list. */
private fun iqrFences(sorted: List<Double>, k: Double): ClosedFloatingPointRange<Double> {
    val q1 = percentile(sorted, 0.25)
    val q3 = percentile(sorted, 0.75)
    val iqr = q3 - q1
    return (q1 - k * iqr)..(q3 + k * iqr)
}

/**
 * Fences `median ± k * (1.4826 * MAD)` over a pre-sorted, non-empty price list. The 1.4826 factor
 * makes MAD a consistent estimator of the standard deviation for normal data. Returns null when the
 * scaled MAD is 0 (half or more of the values are identical) — there is no spread to form a fence,
 * so the caller keeps everything rather than collapsing to the median.
 */
private fun madFences(sorted: List<Double>, k: Double): ClosedFloatingPointRange<Double>? {
    val median = percentile(sorted, 0.5)
    val deviations = sorted.map { abs(it - median) }.sorted()
    val scaledMad = 1.4826 * percentile(deviations, 0.5)
    if (scaledMad == 0.0) return null
    return (median - k * scaledMad)..(median + k * scaledMad)
}

/** Linear-interpolation percentile (R-7 / Excel PERCENTILE) over a pre-sorted, non-empty list. */
private fun percentile(sorted: List<Double>, p: Double): Double {
    if (sorted.size == 1) return sorted[0]
    val rank = p * (sorted.size - 1)
    val low = floor(rank).toInt()
    val high = ceil(rank).toInt()
    if (low == high) return sorted[low]
    val weight = rank - low
    return sorted[low] * (1 - weight) + sorted[high] * weight
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
