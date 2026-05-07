package com.salles.scrapping.db.tables

import com.salles.scrapping.domain.QuantityBase
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object Price : Table("prices") {
    val time            = timestamp("time").defaultExpression(CurrentTimestamp)
    val location        = integer("location").default(0)
    val product         = varchar("product", 100)
    val price           = integer("price")
    val brand           = varchar("brand", 100).default("")
    val quantityBase    = enumerationByName<QuantityBase>("quantity_base", 20).default(QuantityBase.GRAMS)
    val productLabel    = text("product_label").nullable()
    val createdAt       = timestamp("created_at").defaultExpression(CurrentTimestamp)
}
