package com.salles.api.services

import com.salles.api.database.TestDatabase
import com.salles.api.repositories.PostgresProductToScrapRepository
import com.salles.api.data.productToScrap.CreateProductToScrapRequest
import com.salles.domain.QuantityBase
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductToScrapDTOServiceTest {

    private val service = ProductToScrapService(PostgresProductToScrapRepository())

    @BeforeTest
    fun setUp() {
        TestDatabase.reset()
    }

    @Test
    fun `create persists product and returns entity with id`() = runTest {
        val request = CreateProductToScrapRequest(
            name         = "Açúcar",
            search       = "açúcar cristal",
            quantityBase = QuantityBase.GRAMS,
            keyWords     = listOf("açúcar", "cristal"),
        )

        val entity = service.create(request)

        assertEquals("Açúcar", entity.name)
        assertEquals(QuantityBase.GRAMS, entity.quantityBase)
    }

    @Test
    fun `list returns all persisted products`() = runTest {
        service.create(CreateProductToScrapRequest(
            "Açúcar refinado",
            "açúcar refinado",
            QuantityBase.GRAMS,
            listOf("refinado"),
            emptyList()
        ))
        service.create(CreateProductToScrapRequest(
            "Azeite",
            "azeite oliva",
            QuantityBase.MILLILITERS,
            listOf("azeite", "oliva"),
            emptyList()
        ))

        val all = service.list().data.sortedBy { it.name }

        assertEquals(2, all.size)
        assertEquals("Açúcar refinado", all[1].name)
        assertEquals(QuantityBase.GRAMS, all[1].quantityBase)
        assertEquals(listOf("refinado"), all[1].keyWords)
        assertEquals("Azeite", all[0].name)
        assertEquals(QuantityBase.MILLILITERS, all[0].quantityBase)
        assertEquals(listOf("azeite", "oliva"), all[0].keyWords)
    }

    @Test
    fun `list returns empty when no products exist`() = runTest {
        val all = service.list().data
        assertEquals(0, all.size)
    }

    @Test
    fun `listDistinct returns one entry per productName`() = runTest {
        service.create(CreateProductToScrapRequest(
            name         = "Açúcar",
            search       = "açúcar cristal",
            quantityBase = QuantityBase.GRAMS,
            keyWords     = listOf("cristal"),
            denyWords    = emptyList(),
        ))
        service.create(CreateProductToScrapRequest(
            name         = "Açúcar",
            search       = "açúcar refinado",
            quantityBase = QuantityBase.GRAMS,
            keyWords     = listOf("refinado"),
            denyWords    = emptyList(),
        ))
        service.create(CreateProductToScrapRequest(
            name         = "Azeite",
            search       = "azeite oliva",
            quantityBase = QuantityBase.MILLILITERS,
            keyWords     = listOf("azeite"),
            denyWords    = emptyList(),
        ))

        val result = service.listDistinct().data

        assertEquals(2, result.size)
        val names = result.map { it.name }.toSet()
        assertEquals(setOf("Açúcar", "Azeite"), names)
    }

    @Test
    fun `listDistinct returns empty when no products exist`() = runTest {
        val result = service.listDistinct().data
        assertEquals(0, result.size)
    }
}
