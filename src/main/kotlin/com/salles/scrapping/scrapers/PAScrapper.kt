package com.salles.scrapping.scrapers

import com.salles.scrapping.data.PAApiResponse
import com.salles.scrapping.data.PASearchRequest
import com.salles.scrapping.data.PASearchResponse
import com.salles.scrapping.data.ProductToScrap
import com.salles.scrapping.domain.QuantityBase
import com.salles.scrapping.domain.ProductToScrap as DomainProductToScrap
import com.salles.scrapping.domain.Scrapper
import com.salles.scrapping.domain.SearchResponse
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
                ProductToScrap("açúcar", listOf("refinado"), QuantityBase.GRAMS),
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
            val hasKeyword = productToScrap.keyWords.any { product.name.contains(it) }
            if (!hasKeyword) continue

            val brand = (product as? PASearchResponse)?.brand ?: continue
            if (seenBrands.containsKey(brand)) continue

            seenBrands[brand] = true
            result.add(product)
        }
        return result
    }
}
