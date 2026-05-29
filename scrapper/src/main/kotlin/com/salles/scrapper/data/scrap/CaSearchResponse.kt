package com.salles.scrapper.data.scrap

import com.salles.domain.SearchResponse
import java.text.Normalizer
import kotlinx.serialization.Serializable

@Serializable
data class CaSearchResponse(
    override val price: Int? = null,
    override var name: String = "",
    val brand: String = "",
) : SearchResponse {
    init {
        name = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}"), "")
            .lowercase()
    }
}
