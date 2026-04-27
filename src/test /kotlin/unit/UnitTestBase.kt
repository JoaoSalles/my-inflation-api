package com.example.unit

import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll

abstract class UnitTestBase {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        }
    }
}
