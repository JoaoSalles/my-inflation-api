package com.salles.scrapping.data

import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    val data: List<T>,
    val page: Int,
    val pageSize: Int,
    val hasNext: Boolean,
)
