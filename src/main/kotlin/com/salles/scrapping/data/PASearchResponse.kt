package com.salles.scrapping.data

import com.salles.scrapping.domain.SearchResponse
import java.text.Normalizer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class PAApiResponse(
    val products: List<PASearchResponse> = emptyList()
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable()
@JsonIgnoreUnknownKeys
data class PASearchResponse(
    @Serializable(with = PriceSerializer::class)
    override val price: Int? = null,
    override var name: String = "",
    val brand: String = "",
    val unitPriceHomogeneousKit: Double? = null
) : SearchResponse {
    init {
        name = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}"), "")
            .lowercase()
    }
}

private object PriceSerializer : KSerializer<Int> {
    override val descriptor = PrimitiveSerialDescriptor("Price", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeDouble(value / 100.0)
    override fun deserialize(decoder: Decoder): Int = (decoder.decodeDouble() * 100).toInt()
}
