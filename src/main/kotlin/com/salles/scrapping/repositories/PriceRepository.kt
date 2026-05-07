package com.salles.scrapping.repositories

import com.salles.scrapping.db.DatabaseException
import com.salles.scrapping.db.dbQuery
import com.salles.scrapping.db.entities.PriceDailyAvgEntity
import com.salles.scrapping.db.entities.PriceEntity
import com.salles.scrapping.db.tables.Price
import com.salles.scrapping.domain.QuantityBase
import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.avg
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

interface PriceRepository {
    suspend fun create(productName: String, brand: String, price: Int, quantityBase: QuantityBase, location: Int = 0, productLabel: String? = null): PriceEntity
    suspend fun list(
        product: String?,
        from: Instant? = null,
        to: Instant? = null,
        page: Int = 0,
        pageSize: Int = 20
    ): Pair<List<PriceEntity>, Boolean>
    suspend fun listProductPrice(
        product: String,
        from: Instant? = null,
        to: Instant? = null,
        page: Int = 0,
        pageSize: Int = 20,
    ): Pair<List<PriceDailyAvgEntity>, Boolean>
}

class PostgresPriceRepository : PriceRepository {

    override suspend fun create(
        productName: String,
        brand: String,
        price: Int,
        quantityBase: QuantityBase,
        location: Int,
        productLabel: String?,
    ): PriceEntity = try {
        dbQuery {
            val now: Instant = Clock.System.now()

            Price.insert {
                it[Price.time]         = now
                it[Price.product]      = productName
                it[Price.brand]        = brand
                it[Price.price]        = price
                it[Price.quantityBase] = quantityBase
                it[Price.location]     = location
                it[Price.productLabel] = productLabel
                it[Price.createdAt]    = now
            }
            PriceEntity(
                time         = now,
                location     = location,
                product      = productName,
                price        = price,
                brand        = brand,
                quantityBase = quantityBase,
                productLabel = productLabel,
                createdAt    = now,
            )
        }
    } catch (e: Exception) {
        throw DatabaseException(e)
    }

    override suspend fun list(
        product: String?,
        from: Instant?,
        to: Instant?,
        page: Int,
        pageSize: Int,
    ): Pair<List<PriceEntity>, Boolean> = dbQuery {
        val rows = Price.selectAll()
            .apply {
                product?.let { andWhere { Price.product like "$it%".lowercase() } }
                from?.let    { andWhere { Price.time greaterEq it } }
                to?.let      { andWhere { Price.time lessEq   it } }
            }
            .orderBy(Price.time, SortOrder.DESC)
            .limit(pageSize + 1)
            .offset((page * pageSize).toLong())
            .map { row ->
                PriceEntity(
                    time         = row[Price.time],
                    location     = row[Price.location],
                    product      = row[Price.product],
                    price        = row[Price.price],
                    brand        = row[Price.brand],
                    quantityBase = row[Price.quantityBase],
                    productLabel = row[Price.productLabel],
                    createdAt    = row[Price.createdAt],
                )
            }
        if (pageSize == 0) Pair(rows, false)
        else Pair(rows.take(pageSize), rows.size > pageSize)
    }

    override suspend fun listProductPrice(
        product: String,
        from: Instant?,
        to: Instant?,
        page: Int,
        pageSize: Int,
    ): Pair<List<PriceDailyAvgEntity>, Boolean> = dbQuery {
        val dayBucket = CustomFunction<Instant>("time_bucket", Price.time.columnType, stringLiteral("1 day"), Price.time)
        val avgPrice  = Price.price.avg()

        val rows = Price.select(dayBucket, Price.product, avgPrice)
            .apply {
                andWhere { Price.product like "$product%" }
                from?.let { andWhere { Price.time greaterEq it } }
                to?.let   { andWhere { Price.time lessEq   it } }
            }
            .groupBy(dayBucket, Price.product)
            .orderBy(dayBucket to SortOrder.DESC)
            .apply {
                pageSize.let {
                    if (pageSize != 0) {
                        limit(pageSize + 1)
                    }
                }
            }
            .offset((page * pageSize).toLong())
            .map { row ->
                PriceDailyAvgEntity(
                    day      = row[dayBucket],
                    product  = row[Price.product],
                    avgPrice = row[avgPrice]?.toDouble() ?: 0.0,
                )
            }
        if (pageSize == 0) Pair(rows, false)
        else Pair(rows.take(pageSize), rows.size > pageSize)
    }
}
