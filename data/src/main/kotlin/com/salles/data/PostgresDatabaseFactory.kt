package com.salles.data

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import javax.sql.DataSource

class PostgresDatabaseFactory(private val dataSource: DataSource) {

    init {
//        runMigrations(dataSource)
        Database.connect(dataSource)
    }

    fun close() = (dataSource as? java.io.Closeable)?.close()

    constructor(config: ApplicationConfig) : this(
        buildHikariDataSource(config)
    )

    companion object {
        /**
         * Builds a factory straight from environment variables (no Ktor config), for the
         * standalone scraper batch job. Defaults mirror `application.yaml`.
         */
        fun fromEnv(): PostgresDatabaseFactory {
            val hikariConfig = HikariConfig().apply {
                jdbcUrl         = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/my_inflation"
                username        = System.getenv("DB_USER") ?: "salles"
                password        = System.getenv("DB_PASSWORD") ?: "salles"
                maximumPoolSize = (System.getenv("DB_POOL_MAX") ?: "4").toInt()
                driverClassName = "org.postgresql.Driver"
                isAutoCommit    = false
                validate()
            }
            return PostgresDatabaseFactory(HikariDataSource(hikariConfig))
        }

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
