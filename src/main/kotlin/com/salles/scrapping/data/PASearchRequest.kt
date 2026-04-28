package com.salles.scrapping.data

import kotlinx.serialization.Serializable

@Serializable
data class PASearchRequest(
    val terms: String,
    val page: Int = 1,
    val sortBy: String = "relevance",
    val resultsPerPage: Int = 21,
    val allowRedirect: Boolean = true,
    val storeId: Int = 461,
    val department: String = "ecom",
    val partner: String = "fallback"
)
