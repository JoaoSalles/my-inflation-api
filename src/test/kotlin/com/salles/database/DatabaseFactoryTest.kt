package com.salles.database

import com.salles.database.tables.PriceSnapshots
import kotlinx.coroutines.test.runTest
import org.h2.jdbcx.JdbcDataSource
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DatabaseFactoryTest {

    @BeforeTest
    fun setUp() {
        val dataSource = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:test_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
            user = "sa"
            password = ""
        }
        DatabaseFactory(dataSource)
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
                it[quantityBase] = "GRAMS"
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
        assertEquals("GRAMS", row[PriceSnapshots.quantityBase])
        assertNotNull(row[PriceSnapshots.scrapedAt])
    }
}
