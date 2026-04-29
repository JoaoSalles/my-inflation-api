package com.salles.database.tables

import com.salles.scrapping.domain.QuantityBase
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object ProductsToScrap : Table("products_to_scrap") {
    val id           = long("id").autoIncrement()
    val productName  = varchar("product_name", 250).uniqueIndex()
    val quantityBase = enumerationByName<QuantityBase>("quantity_base", 20).default(QuantityBase.GRAMS)
    val keywords     = text("keywords").default("[]")
    val createdAt    = timestamp("created_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
