package com.salles.scrapping.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import javax.sql.DataSource

class PostgresDatabaseFactory(dataSource: DataSource) {

    init {
        runMigrations(dataSource)
        Database.connect(dataSource)
    }

    constructor(config: ApplicationConfig) : this(buildHikariDataSource(config))

    companion object {
        private fun buildHikariDataSource(config: ApplicationConfig): HikariDataSource {
            val hikariConfig = HikariConfig().apply {
                jdbcUrl         = config.property("db.url").getString()
                username        = config.property("db.user").getString()
                password        = config.property("db.password").getString()
                maximumPoolSize = config.property("db.pool.maximumPoolSize").getString().toInt()
                driverClassName = "org.postgresql.Driver"
                isAutoCommit    = false
                validate()
            }
            return HikariDataSource(hikariConfig)
        }

        private fun runMigrations(dataSource: DataSource) {
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate()
        }
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    suspendTransaction { block() }
