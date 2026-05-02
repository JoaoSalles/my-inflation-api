package com.salles.scrapping.repositories

import com.salles.scrapping.db.DatabaseException
import com.salles.scrapping.db.dbQuery
import com.salles.scrapping.db.entities.PriceEntity
import com.salles.scrapping.db.tables.Price
import com.salles.scrapping.domain.QuantityBase
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import kotlin.time.Instant
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

interface PriceRepository {
    suspend fun create(productName: String, brand: String, price: Int, quantityBase: QuantityBase, location: Int = 0): PriceEntity
    suspend fun list(from: Instant? = null, to: Instant? = null, page: Int = 0, pageSize: Int = 20): List<PriceEntity>
}

class PostgresPriceRepository : PriceRepository {

    override suspend fun create(
        productName: String,
        brand: String,
        price: Int,
        quantityBase: QuantityBase,
        location: Int,
    ): PriceEntity = try {
        dbQuery {
            val now = kotlin.time.Clock.System.now()
            Price.insert {
                it[Price.time]         = now
                it[Price.product]      = productName
                it[Price.brand]        = brand
                it[Price.price]        = price
                it[Price.quantityBase] = quantityBase
                it[Price.location]     = location
            }

            PriceEntity(
                time         = now,
                location     = location,
                product      = productName,
                price        = price,
                brand        = brand,
                quantityBase = quantityBase,
                createdAt    = now,
            )
        }
    } catch (e: Exception) {
        throw DatabaseException(e)
    }

    override suspend fun list(
        from: Instant?,
        to: Instant?,
        page: Int,
        pageSize: Int,
    ): List<PriceEntity> = dbQuery {
        Price.selectAll()
            .apply {
                if (from != null) andWhere { Price.time greaterEq from }
                if (to != null) andWhere { Price.time lessEq to }
            }
            .limit(pageSize)
            .offset((page * pageSize).toLong())
            .map { row ->
                PriceEntity(
                    time         = row[Price.time],
                    location     = row[Price.location],
                    product      = row[Price.product],
                    price        = row[Price.price],
                    brand        = row[Price.brand],
                    quantityBase = row[Price.quantityBase],
                    createdAt    = row[Price.createdAt],
                )
            }
    }
}