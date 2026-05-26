package com.salles.core.database

import com.salles.data.PostgresDatabaseFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

object TestDatabase {
    private val container: PostgreSQLContainer<*> =
        PostgreSQLContainer(
            DockerImageName.parse("timescale/timescaledb:latest-pg16")
                .asCompatibleSubstituteFor("postgres")
        ).apply { start() }

    init {
        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl         = container.jdbcUrl
            username        = container.username
            password        = container.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 2
            isAutoCommit    = false
            validate()
        })
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load()
            .migrate()
        PostgresDatabaseFactory(dataSource)
    }

    fun reset() = transaction {
        exec("DELETE FROM prices")
        exec("DELETE FROM products_to_scrap")
    }
}
