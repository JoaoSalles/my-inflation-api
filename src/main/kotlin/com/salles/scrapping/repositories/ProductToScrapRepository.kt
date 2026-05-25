package com.salles.scrapping.repositories

import com.salles.scrapping.data.productToScrap.ProductToScrapCreateResponse
import com.salles.scrapping.data.productToScrap.ProductToScrapDTO
import com.salles.scrapping.db.DatabaseException
import com.salles.scrapping.db.ProductNameAlreadyExistsException
import com.salles.scrapping.db.dbQuery
import com.salles.scrapping.db.tables.ProductsToScrap
import com.salles.scrapping.domain.QuantityBase
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.sql.SQLException

interface ProductToScrapRepository {
    suspend fun create(productName: String, search: String, quantityBase: QuantityBase, keyWords: List<String>, denyWords: List<String>): ProductToScrapCreateResponse
    suspend fun update(id: Long, productName: String, search: String, quantityBase: QuantityBase, keyWords: List<String>, denyWords: List<String>): ProductToScrapCreateResponse?
    suspend fun list(product: String? = null): Pair<List<ProductToScrapDTO>, Boolean>
    suspend fun listDistinct(): Pair<List<ProductToScrapCreateResponse>, Boolean>
}

class PostgresProductToScrapRepository : ProductToScrapRepository {

    override suspend fun create(
        productName: String,
        search: String,
        quantityBase: QuantityBase,
        keyWords: List<String>,
        denyWords: List<String>,
    ): ProductToScrapCreateResponse = try {
        dbQuery {
            val insertedId = ProductsToScrap.insert {
                it[ProductsToScrap.productName]  = productName
                it[ProductsToScrap.search]       = search
                it[ProductsToScrap.quantityBase] = quantityBase
                it[ProductsToScrap.keyWords]     = Json.encodeToString(keyWords)
                it[ProductsToScrap.denyWords]     = Json.encodeToString(denyWords)
            } get ProductsToScrap.id

            ProductToScrapCreateResponse(
                name  = productName,
                quantityBase = quantityBase
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
        search: String,
        quantityBase: QuantityBase,
        keyWords: List<String>,
        denyWords: List<String>,
    ): ProductToScrapCreateResponse? = dbQuery {
        val updated = ProductsToScrap.update({ ProductsToScrap.id eq id }) {
            it[ProductsToScrap.productName]  = productName
            it[ProductsToScrap.search]       = search
            it[ProductsToScrap.quantityBase] = quantityBase
            it[ProductsToScrap.keyWords]     = Json.encodeToString(keyWords)
            it[ProductsToScrap.denyWords]    = Json.encodeToString(denyWords)
        }
        if (updated == 0) return@dbQuery null
        ProductToScrapCreateResponse(
            name  = productName,
            quantityBase = quantityBase
        )
    }

    override suspend fun list(product: String?): Pair<List<ProductToScrapDTO>, Boolean> = dbQuery {
        val query = if (product != null)
            ProductsToScrap.selectAll().where { ProductsToScrap.productName eq product }
        else
            ProductsToScrap.selectAll()

        val rows = query.map { row ->
            ProductToScrapDTO(
                name  = row[ProductsToScrap.productName],
                search       = row[ProductsToScrap.search],
                quantityBase = row[ProductsToScrap.quantityBase],
                keyWords     = Json.decodeFromString(row[ProductsToScrap.keyWords]),
                denyWords    = Json.decodeFromString(row[ProductsToScrap.denyWords])
            )
        }
        Pair(rows, false)
    }

    override suspend fun listDistinct(): Pair<List<ProductToScrapCreateResponse>, Boolean> = dbQuery {
        val rows = ProductsToScrap.selectAll()
            .orderBy(ProductsToScrap.productName to SortOrder.ASC)
            .map { row ->
                ProductToScrapCreateResponse(
                    name = row[ProductsToScrap.productName],
                    quantityBase = row[ProductsToScrap.quantityBase]
                )
            }
            .distinctBy { it.name }
        Pair(rows, false)
    }
}
