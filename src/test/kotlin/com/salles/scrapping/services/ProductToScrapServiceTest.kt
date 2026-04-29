package com.salles.scrapping.services

import com.salles.database.TestDatabase
import com.salles.database.repositories.PostgresProductToScrapRepository
import com.salles.scrapping.data.CreateProductToScrapRequest
import com.salles.scrapping.domain.QuantityBase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductToScrapServiceTest {

    private val service = ProductToScrapService(PostgresProductToScrapRepository())

    @BeforeTest
    fun setUp() {
        TestDatabase.reset()
    }

    @Test
    fun `create persists product and returns entity with id`() = runTest {
        val request = CreateProductToScrapRequest(
            productName  = "Açúcar",
            quantityBase = QuantityBase.GRAMS,
            keyWords     = listOf("açúcar", "cristal"),
        )

        val entity = service.create(request)

        assertEquals("Açúcar", entity.productName)
        assertEquals(QuantityBase.GRAMS, entity.quantityBase)
        assertEquals(listOf("açúcar", "cristal"), entity.keyWords)
    }

    @Test
    fun `list returns all persisted products`() = runTest {
        service.create(CreateProductToScrapRequest("Açúcar", QuantityBase.GRAMS, listOf("açúcar")))
        service.create(CreateProductToScrapRequest("Azeite", QuantityBase.MILLILITERS, listOf("azeite", "oliva")))

        val all = service.list().sortedBy { it.id }

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
