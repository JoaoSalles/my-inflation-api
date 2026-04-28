package com.salles.scrapping.scrapers

import com.salles.scrapping.data.PAApiResponse
import com.salles.scrapping.data.PASearchRequest
import com.salles.scrapping.data.PASearchResponse
import com.salles.scrapping.data.ProductToScrap
import com.salles.scrapping.domain.QuantityBase
import com.salles.scrapping.domain.ProductToScrap as DomainProductToScrap
import com.salles.scrapping.domain.Scrapper
import com.salles.scrapping.domain.SearchResponse
import com.salles.scrapping.utils.normalizeForMillicent
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(PAScrapper::class.java)

class PAScrapper(
    private val client: HttpClient,
) : Scrapper<PASearchResponse> {

    override suspend fun scrap(product: String): List<PASearchResponse> {
        try {
            val response: HttpResponse = client.post("https://api.vendas.gpa.digital/pa/search/search") {
                contentType(ContentType.Application.Json)
                setBody(PASearchRequest(product))
            }
            if (!response.status.isSuccess()) {
                log.error("PA search failed for term='$product': HTTP ${response.status}")
                return emptyList()
            }
            val products = response.body<PAApiResponse>().products.filter { it.unitPriceHomogeneousKit == null }
            this.parseProducts(
                ProductToScrap("açucar", listOf("refinado"), QuantityBase.GRAMS),
                products
                )
            log.info("PA search for term='$product' returned ${products.size} products: ${products}")
            return products
        } catch (e: Exception) {
            log.error("PA search error for term='$product'", e)
            return emptyList()
        }
    }

    override suspend fun parseProducts(
        productToScrap: DomainProductToScrap,
        products: List<SearchResponse>
    ): List<SearchResponse> {
        val seenBrands = mutableMapOf<String, Boolean>()
        val result = mutableListOf<SearchResponse>()

        for (product in products) {
            if (product.name.isEmpty()) continue

            val hasKeyword = productToScrap.keyWords.all { product.name.contains(it) }

            if (!hasKeyword) continue

            val brand = (product as? PASearchResponse)?.brand ?: continue
            if (seenBrands.containsKey(brand)) continue

            var parsedProduct: PASearchResponse;
            when (productToScrap.quantityBase) {
                QuantityBase.GRAMS -> {
                    val pricePerGram = this.parseProductsPerGram(
                        productToScrap,
                        product,
                    )

                    if (pricePerGram == 0) continue

                    parsedProduct = PASearchResponse(
                        pricePerGram,
                        productToScrap.name,
                        brand
                    )
                }
                QuantityBase.UNITS -> {
                    parsedProduct = PASearchResponse(
                        product.price,
                        productToScrap.name,
                        brand
                    )
                }
                QuantityBase.MILLILITERS -> {
                    parsedProduct = PASearchResponse(
                        product.price,
                        productToScrap.name,
                        brand
                    )
                }
            }

            seenBrands[brand] = true
            result.add(parsedProduct)
        }
        return result
    }

    /*
    * There is a problem using grams and milliliter, it may cost less than a cent
    * so the integer value will be a representation of original value divided by 10000
    * */

    suspend fun parseProductsPerGram(
        productName: DomainProductToScrap,
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

        return (normalizeForMillicent((product.price ?: 0) / grams))
    }

    suspend fun parseProductsPerLiter(
        productName: DomainProductToScrap,
        product: SearchResponse
    ): Int {
        return 0;
    }
}
