package com.salles.database

import org.h2.jdbcx.JdbcDataSource
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object TestDatabase {
    init {
        val dataSource = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:test_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
            user = "sa"
            password = ""
        }
        PostgresDatabaseFactory(dataSource)
    }

    fun reset() = transaction {
        exec("DELETE FROM products_to_scrap")
        exec("DELETE FROM price_snapshots")
    }
}
