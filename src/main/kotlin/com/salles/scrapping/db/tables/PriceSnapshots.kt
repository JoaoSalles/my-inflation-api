package com.salles.scrapping.db.tables

import com.salles.scrapping.domain.QuantityBase
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object PriceSnapshots : Table("price_snapshots") {
    val id           = long("id").autoIncrement()
    val productName  = varchar("product_name", 255)
    val brand        = varchar("brand", 255)
    val price        = integer("price")
    val quantityBase = enumerationByName<QuantityBase>("quantity_base", 20).default(QuantityBase.GRAMS)
    val scrapedAt    = timestamp("scraped_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
