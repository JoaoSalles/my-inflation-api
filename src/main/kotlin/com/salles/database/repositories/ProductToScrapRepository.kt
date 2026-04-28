package com.salles.database.repositories

import com.salles.database.dbQuery
import com.salles.database.entities.ProductToScrapEntity
import com.salles.database.tables.ProductsToScrap
import com.salles.scrapping.domain.QuantityBase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

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
    ): ProductToScrapEntity = dbQuery {
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
