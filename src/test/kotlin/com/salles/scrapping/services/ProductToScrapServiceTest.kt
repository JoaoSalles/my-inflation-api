package com.salles.scrapping.services

import com.salles.database.entities.ProductToScrapEntity
import com.salles.database.repositories.ProductToScrapRepository
import com.salles.scrapping.data.CreateProductToScrapRequest
import com.salles.scrapping.domain.QuantityBase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductToScrapServiceTest {

    private val fakeRepo = FakeProductToScrapRepository()
    private val service  = ProductToScrapService(fakeRepo)

    @Test
    fun `create persists product and returns entity with id`() = runTest {
        val request = CreateProductToScrapRequest(
            productName  = "Açúcar",
            quantityBase = QuantityBase.GRAMS,
            keyWords     = listOf("açúcar", "cristal"),
        )

        val entity = service.create(request)

        assertEquals(1L, entity.id)
        assertEquals("Açúcar", entity.productName)
        assertEquals(QuantityBase.GRAMS, entity.quantityBase)
        assertEquals(listOf("açúcar", "cristal"), entity.keyWords)
    }

    @Test
    fun `list returns all persisted products`() = runTest {
        service.create(CreateProductToScrapRequest("Açúcar", QuantityBase.GRAMS, listOf("açúcar")))
        service.create(CreateProductToScrapRequest("Azeite", QuantityBase.MILLILITERS, listOf("azeite", "oliva")))

        val all = service.list()

        assertEquals(2, all.size)
        assertEquals("Açúcar", all[0].productName)
        assertEquals(QuantityBase.GRAMS, all[0].quantityBase)
        assertEquals(listOf("açúcar"), all[0].keyWords)
        assertEquals("Azeite", all[1].productName)
        assertEquals(QuantityBase.MILLILITERS, all[1].quantityBase)
        assertEquals(listOf("azeite", "oliva"), all[1].keyWords)
    }

    @Test
    fun `list returns empty when no products exist`() = runTest {
        val all = service.list()
        assertEquals(0, all.size)
    }
}

private class FakeProductToScrapRepository : ProductToScrapRepository {
    private val store = mutableListOf<ProductToScrapEntity>()
    private var nextId = 1L

    override suspend fun create(
        productName: String,
        quantityBase: QuantityBase,
        keyWords: List<String>,
    ): ProductToScrapEntity {
        val entity = ProductToScrapEntity(nextId++, productName, quantityBase, keyWords)
        store.add(entity)
        return entity
    }

    override suspend fun update(
        id: Long,
        productName: String,
        quantityBase: QuantityBase,
        keyWords: List<String>,
    ): ProductToScrapEntity? = TODO("not yet used by service")

    override suspend fun list(): List<ProductToScrapEntity> = store.toList()
}
