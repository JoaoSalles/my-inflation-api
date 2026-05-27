package com.salles.data.tables

import com.salles.domain.QuantityBase
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object ProductsToScrap : Table("products_to_scrap") {
    val id           = long("id").autoIncrement()
    val productName  = varchar("product_name", 250)
    val search       = varchar("search", 250)
    val quantityBase = enumerationByName<QuantityBase>(
        "quantity_base",
        20
    ).default(QuantityBase.GRAMS)
    val keyWords     = text("key_words").default("[]")
    val denyWords    = text("deny_words").default("[]")
    val createdAt    = timestamp("created_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
