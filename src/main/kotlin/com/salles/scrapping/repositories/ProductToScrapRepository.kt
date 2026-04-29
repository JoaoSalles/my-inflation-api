package com.salles.scrapping.repositories

import com.salles.scrapping.db.DatabaseException
import com.salles.scrapping.db.ProductNameAlreadyExistsException
import com.salles.scrapping.db.dbQuery
import com.salles.scrapping.db.entities.ProductToScrapEntity
import com.salles.scrapping.db.tables.ProductsToScrap
import com.salles.scrapping.domain.QuantityBase
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.sql.SQLException

interface ProductToScrapRepository {
    suspend fun create(productName: String, quantityBase: QuantityBase, keyWords: List<String>): ProductToScrapEntity
    suspend fun update(id: Long, productName: String, quantityBase: QuantityBase, keyWords: List<String>): ProductToScrapEntity?
    suspend fun list(): List<ProductToScrapEntity>
}

class PostgresProductToScrapRepository : ProductToScrapRepository {

    override suspend fun create(
        productName: String,
        quantityBase: QuantityBase,
        keyWords: List<String>,
    ): ProductToScrapEntity = try {
        dbQuery {
            val insertedId = ProductsToScrap.insert {
                it[ProductsToScrap.productName]  = productName
                it[ProductsToScrap.quantityBase] = quantityBase
                it[ProductsToScrap.keywords]     = Json.encodeToString(keyWords)
            } get ProductsToScrap.id

            ProductToScrapEntity(
                id           = insertedId,
                productName  = productName,
                quantityBase = quantityBase,
                keyWords     = keyWords,
            )
        }
    } catch (e: Exception) {
        val sqlState = generateSequence(e.cause) { it.cause }
            .filterIsInstance<SQLException>()
            .firstOrNull()
            ?.sqlState
        if (sqlState == "23505") throw ProductNameAlreadyExistsException(productName)
        throw DatabaseException(e)
    }

    override suspend fun update(
        id: Long,
        productName: String,
        quantityBase: QuantityBase,
        keyWords: List<String>,
    ): ProductToScrapEntity? = dbQuery {
        val updated = ProductsToScrap.update({ ProductsToScrap.id eq id }) {
            it[ProductsToScrap.productName]  = productName
            it[ProductsToScrap.quantityBase] = quantityBase
            it[ProductsToScrap.keywords]     = Json.encodeToString(keyWords)
        }
        if (updated == 0) return@dbQuery null
        ProductToScrapEntity(id = id, productName = productName, quantityBase = quantityBase, keyWords = keyWords)
    }

    override suspend fun list(): List<ProductToScrapEntity> = dbQuery {
        ProductsToScrap.selectAll().map { row ->
            ProductToScrapEntity(
                id           = row[ProductsToScrap.id],
                productName  = row[ProductsToScrap.productName],
                quantityBase = row[ProductsToScrap.quantityBase],
                keyWords     = Json.decodeFromString(row[ProductsToScrap.keywords]),
            )
        }
    }
}
