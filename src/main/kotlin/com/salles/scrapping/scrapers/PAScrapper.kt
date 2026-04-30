package com.salles.scrapping.scrapers

import com.salles.scrapping.data.PAApiResponse
import com.salles.scrapping.data.PASearchRequest
import com.salles.scrapping.data.PASearchResponse
import com.salles.scrapping.data.ProductToScrap
import com.salles.scrapping.db.entities.ProductToScrapEntity
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

    override suspend fun scrap(product: ProductToScrapEntity): List<PASearchResponse> {
        try {
            val response: HttpResponse = client.post("https://api.vendas.gpa.digital/pa/search/search") {
                contentType(ContentType.Application.Json)
                setBody(PASearchRequest(product.productName))
            }
            if (!response.status.isSuccess()) {
                log.error("PA search failed for term='$product': HTTP ${response.status}")
                return emptyList()
            }
            val products = response.body<PAApiResponse>().products.filter { it.unitPriceHomogeneousKit == null }

            val parsedProducts = this.parseProducts(
                ProductToScrap(product.productName, product.keyWords, product.denyWords, product.quantityBase),
                products
                )
            log.info("PA search for term='${product.productName}' returned ${parsedProducts.size} products: ${parsedProducts}")
            return products
        } catch (e: Exception) {
            // TODO create a flow for errors, to keep track of scrapping that failed
            // retry would be nice
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

            var hasKeyword = true;
            var hasDenyword = false
            if (!productToScrap.keyWords.isEmpty()) {
                hasKeyword = productToScrap.keyWords.all { product.name.contains(it) }
            }
            if (!productToScrap.denyWords.isEmpty()) {
                 hasDenyword = productToScrap.denyWords.all { product.name.contains(it) }
            }

            if (!hasKeyword || hasDenyword) continue

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
                    val pricePerUnit = this.parseProductsPerUnits(productToScrap, product)

                    if (pricePerUnit == 0) continue

                    parsedProduct = PASearchResponse(
                        pricePerUnit,
                        productToScrap.name,
                        brand
                    )
                }
                QuantityBase.MILLILITERS -> {
                    val pricePerMilliliter = this.parseProductsPerMilliliters(
                        productToScrap,
                        product,
                    )

                    if (pricePerMilliliter == 0) continue

                    parsedProduct = PASearchResponse(
                        pricePerMilliliter,
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

        return normalizeForMillicent((product.price ?: 0) / grams)
    }

    suspend fun parseProductsPerUnits(
        productName: DomainProductToScrap,
        product: SearchResponse
    ): Int {
        val unidadeRegex = Regex("""(\d+)\s*[Uu]nidades?""")
        val units = unidadeRegex.find(product.name)?.groupValues?.get(1)?.toIntOrNull() ?: return 0
        return (product.price ?: 0) / units
    }

    suspend fun parseProductsPerMilliliters(
        productName: DomainProductToScrap,
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
