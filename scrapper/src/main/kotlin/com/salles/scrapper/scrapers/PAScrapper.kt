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
import com.salles.domain.services.PriceServiceInterface
import com.salles.scrapper.utils.containsDenyword
import com.salles.scrapper.utils.matchesKeywords
import com.salles.scrapper.utils.pricePerQuantity
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(PAScrapper::class.java)


class PAScrapper(
    private val client: HttpClient,
    private val priceService: PriceServiceInterface? = null,
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
            if (!matchesKeywords(product.name, productToScrap.keyWords) ||
                containsDenyword(product.name, productToScrap.denyWords, productToScrap.quantityBase)) continue

            val brand = (product as? PASearchResponse)?.brand ?: continue

            val price = pricePerQuantity(productToScrap.quantityBase, product)
            if (price == 0) continue

            val name = if (productToScrap.quantityBase == QuantityBase.GRAMS) product.name else productToScrap.name
            result.add(PASearchResponse(price, name, brand))
        }
        return result
    }
}
