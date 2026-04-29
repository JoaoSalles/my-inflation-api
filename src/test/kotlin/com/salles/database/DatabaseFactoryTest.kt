package com.salles.database

import com.salles.scrapping.db.dbQuery
import com.salles.scrapping.db.tables.PriceSnapshots
import com.salles.scrapping.domain.QuantityBase
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PostgresDatabaseFactoryTest {

    @BeforeTest
    fun setUp() {
        TestDatabase.reset()
    }

    @Test
    fun `migrations create price_snapshots table`() = runTest {
        val count = dbQuery {
            PriceSnapshots.selectAll().count()
        }
        assertEquals(0L, count)
    }

    @Test
    fun `can insert and read back a price snapshot`() = runTest {
        val insertedId = dbQuery {
            PriceSnapshots.insert {
                it[productName] = "acucar"
                it[brand] = "uniao"
                it[price] = 300
                it[quantityBase] = QuantityBase.GRAMS
            } get PriceSnapshots.id
        }

        val row = dbQuery {
            PriceSnapshots.selectAll()
                .where { PriceSnapshots.id eq insertedId }
                .single()
        }

        assertEquals("acucar", row[PriceSnapshots.productName])
        assertEquals("uniao", row[PriceSnapshots.brand])
        assertEquals(300, row[PriceSnapshots.price])
        assertEquals(QuantityBase.GRAMS, row[PriceSnapshots.quantityBase])
        assertNotNull(row[PriceSnapshots.scrapedAt])
    }

    @Test
    fun `quantityBase defaults to GRAMS when omitted`() = runTest {
        val insertedId = dbQuery {
            PriceSnapshots.insert {
                it[productName] = "acucar"
                it[brand] = "uniao"
                it[price] = 100
            } get PriceSnapshots.id
        }

        val row = dbQuery {
            PriceSnapshots.selectAll()
                .where { PriceSnapshots.id eq insertedId }
                .single()
        }

        assertEquals(QuantityBase.GRAMS, row[PriceSnapshots.quantityBase])
    }
}
