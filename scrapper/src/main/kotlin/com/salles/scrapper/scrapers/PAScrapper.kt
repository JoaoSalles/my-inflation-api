package com.salles.scrapper.scrapers

import com.salles.scrapper.data.price.CreatePriceCommand
import com.salles.scrapper.data.scrap.PAApiResponse
import com.salles.scrapper.data.scrap.PASearchRequest
import com.salles.scrapper.data.scrap.PASearchResponse
import com.salles.scrapper.data.price.PriceDTO
import com.salles.scrapper.data.productToScrap.ProductToScrapDTO
import com.salles.domain.QuantityBase
import com.salles.domain.scrapper.Scrapper
import com.salles.domain.SearchResponse
import com.salles.scrapper.services.PriceService
import com.salles.scrapper.utils.normalizeForMillicent
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(PAScrapper::class.java)


class PAScrapper(
    private val client: HttpClient,
    private val priceService: PriceService? = null,
) : Scrapper<PASearchResponse, ProductToScrapDTO> {

    override suspend fun scrap(product: ProductToScrapDTO): List<PASearchResponse> {
        try {
            val response: HttpResponse = client.post("https://api.vendas.gpa.digital/pa/search/search") {
                contentType(ContentType.Application.Json)
                setBody(PASearchRequest(product.search ?: ""))
            }
            if (!response.status.isSuccess()) {
                log.error("PA search failed for term='$product': HTTP ${response.status}")
                return emptyList()
            }
            val products = response.body<PAApiResponse>().products.filter { it.unitPriceHomogeneousKit == null }

            val parsedProducts = this.parseProducts(
                ProductToScrapDTO(
                    product.name,
                    product.search ?: "",
                    product.keyWords ?: emptyList(),
                    product.denyWords ?: emptyList(),
                    product.quantityBase
                ),
                products
                )

            parsedProducts.forEach { parsed ->
                try {
                    priceService?.create(
                        CreatePriceCommand(
                            name = product.name,
                            brand = parsed.brand,
                            price = parsed.price ?: 0,
                            quantityBase = product.quantityBase,
                            location = 0,
                            productLabel = product.name.take(80),
                        )
                    )
                } catch (e: Exception) {
                    // TODO create a flow for errors, to keep track of scrapping that failed
                    log.error("Failed to save price for '${product.name}' - brand: ${parsed.brand}", e)
                }
            }
            return parsedProducts
        } catch (e: Exception) {
            // TODO create a flow for errors, to keep track of scrapping that failed
            // retry would be nice
            log.error("PA search error for term='$product'", e)
            return emptyList()
        }
    }

    override suspend fun parseProducts(
        productToScrap: ProductToScrapDTO,
        products: List<SearchResponse>
    ): List<PASearchResponse> {
        val result = mutableListOf<PASearchResponse>()
        for (product in products) {
            if (product.name.isEmpty()) continue

            var hasKeyword = true
            var hasDenyword = false
            val keyWords = productToScrap.keyWords
            val denyWords = productToScrap.denyWords
            if (!keyWords.isNullOrEmpty()) {
                hasKeyword = keyWords.all { product.name.lowercase().contains(it.lowercase()) }
            }
            if (!denyWords.isNullOrEmpty()) {
                hasDenyword = denyWords.any { product.name.lowercase().contains(it.lowercase()) }
            }

            if (!hasKeyword || hasDenyword) continue

            val brand = (product as? PASearchResponse)?.brand ?: continue

            var parsedProduct: PASearchResponse
            when (productToScrap.quantityBase) {
                QuantityBase.GRAMS -> {
                    val pricePerGram = this.parseProductsPerGram(
                        product,
                    )

                    if (pricePerGram == 0) continue

                    parsedProduct = PASearchResponse(
                        pricePerGram,
                        product.name,
                        brand
                    )
                }
                QuantityBase.UNITS -> {
                    val pricePerUnit = this.parseProductsPerUnits(product)

                    if (pricePerUnit == 0) continue

                    parsedProduct = PASearchResponse(
                        pricePerUnit,
                        productToScrap.name,
                        brand
                    )
                }
                QuantityBase.MILLILITERS -> {
                    val pricePerMilliliter = this.parseProductsPerMilliliters(
                        product
                    )

                    if (pricePerMilliliter == 0) continue

                    parsedProduct = PASearchResponse(
                        pricePerMilliliter,
                        productToScrap.name,
                        brand
                    )
                }
            }

            result.add(parsedProduct)
        }
        return result
    }

    /*
    * There is a problem using grams and milliliter, it may cost less than a cent
    * so the integer value will be a representation of original value divided by 10000
    * */

    fun parseProductsPerGram(
        product: SearchResponse
    ): Int {
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

    fun parseProductsPerUnits(
        product: SearchResponse
    ): Int {
        val unidadeRegex = Regex("""(\d+)\s*[Uu]nidades?""")
        val units = unidadeRegex.find(product.name)?.groupValues?.get(1)?.toIntOrNull() ?: return 1
        return ((product.price ?: 0) / units) * 10
    }

    fun parseProductsPerMilliliters(
        product: SearchResponse
    ): Int {
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
}
