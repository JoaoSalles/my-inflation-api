package com.salles.scrapping.utils

fun normalizeForMillicent(value: Number): Int = (value.toDouble() * 10000).toInt()

fun denormalizeFromMillicent(value: Int): Double = value / 10000.0
