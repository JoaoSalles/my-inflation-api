package com.salles.scrapping.repositories

import com.salles.scrapping.data.price.PriceAVGResponse
import com.salles.scrapping.data.price.PriceDTO
import com.salles.scrapping.db.DatabaseException
import com.salles.scrapping.db.dbQuery
import com.salles.scrapping.db.tables.Price
import com.salles.scrapping.domain.QuantityBase
import com.salles.scrapping.domain.price.PriceInterface as PriceModel
import com.salles.scrapping.domain.price.PriceAvgInterface
import com.salles.scrapping.domain.repositories.PrinceRepositoryInterface
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

class PostgresPriceRepository : PrinceRepositoryInterface {

    override suspend fun create(
        productName: String,
        brand: String,
        price: Int,
        quantityBase: QuantityBase,
        location: Int,
        productLabel: String?,
    ): PriceModel = try {
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
            PriceDTO(
                location     = location,
                name      = productName,
                price        = price,
                brand        = brand,
                quantityBase = quantityBase,
                productLabel = productLabel,
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
    ): Pair<List<PriceDTO>, Boolean> = dbQuery {
        val rows = Price.selectAll()
            .apply {
                product?.let { andWhere { Price.product like "$it%".lowercase() } }
                from?.let    { andWhere { Price.time greaterEq it } }
                to?.let      { andWhere { Price.time lessEq   it } }
            }
            .orderBy(Price.time, SortOrder.DESC)
            .apply { if (pageSize != 0) limit(pageSize + 1) }
            .offset((page * pageSize).toLong())
            .map { row ->
                PriceDTO(
                    location     = row[Price.location],
                    name      = row[Price.product],
                    price        = row[Price.price],
                    brand        = row[Price.brand],
                    quantityBase = row[Price.quantityBase],
                    productLabel = row[Price.productLabel],
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
    ): Pair<List<PriceAvgInterface>, Boolean> = dbQuery {
        val dayBucket = CustomFunction<Instant>("time_bucket", Price.time.columnType, stringLiteral("1 day"), Price.time)
        val avgPrice  = Price.price.avg()
        val rows = Price.select(dayBucket, Price.product, avgPrice)
            .apply {
                andWhere { Price.product like "$product%" }
                from?.let { andWhere { Price.time greaterEq it } }
                to?.let   { andWhere { Price.time lessEq   it } }
            }
            .groupBy(dayBucket, Price.product)
            .orderBy(dayBucket to SortOrder.ASC)
            .apply { if (pageSize != 0) limit(pageSize + 1) }
            .offset((page * pageSize).toLong())
            .map { row ->
                PriceAVGResponse(
                    day = row[dayBucket],
                    product = row[Price.product],
                    avgPrice = row[avgPrice]?.toInt() ?: 0,
                )
            }
        if (pageSize == 0) Pair(rows, false)
        else Pair(rows.take(pageSize), rows.size > pageSize)
    }
}