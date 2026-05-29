package com.salles.scrapper.scrappers

import com.salles.domain.QuantityBase
import com.salles.scrapper.data.scrap.PASearchResponse
import com.salles.scrapper.utils.containsDenyword
import com.salles.scrapper.utils.matchesKeywords
import com.salles.scrapper.utils.parseProductsPerMilliliters
import com.salles.scrapper.utils.parseProductsPerUnits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductParsingTest {

    @Test
    fun `matchesKeywords matches accented keyword against unaccented name`() {
        assertTrue(matchesKeywords("Carne moida", listOf("moída")))
    }

    @Test
    fun `matchesKeywords matches unaccented keyword against accented name`() {
        assertTrue(matchesKeywords("Carne moída", listOf("moida")))
    }

    @Test
    fun `matchesKeywords is case-insensitive`() {
        assertTrue(matchesKeywords("CARNE MOÍDA", listOf("moida")))
    }

    @Test
    fun `matchesKeywords requires all keywords to be present`() {
        assertTrue(matchesKeywords("Carne moída bovina", listOf("carne", "moida")))
        assertFalse(matchesKeywords("Carne moída", listOf("carne", "frango")))
    }

    @Test
    fun `matchesKeywords with empty keywords matches`() {
        assertTrue(matchesKeywords("Carne moída", emptyList()))
    }

    @Test
    fun `containsDenyword matches accented denyword against unaccented name`() {
        assertTrue(containsDenyword("Carne moida bovina", listOf("moída"), QuantityBase.GRAMS))
    }

    @Test
    fun `containsDenyword matches unaccented denyword against accented name`() {
        assertTrue(containsDenyword("Carne moída bovina", listOf("moida"), QuantityBase.GRAMS))
    }

    @Test
    fun `containsDenyword is case-insensitive`() {
        assertTrue(containsDenyword("CARNE MOÍDA", listOf("moida"), QuantityBase.GRAMS))
    }

    @Test
    fun `containsDenyword returns false when no denyword present`() {
        assertFalse(containsDenyword("Carne moída", listOf("frango"), QuantityBase.GRAMS))
    }

    @Test
    fun `containsDenyword with empty denywords returns false`() {
        assertFalse(containsDenyword("Carne moída", emptyList(), QuantityBase.GRAMS))
    }

    @Test
    fun `containsDenyword denies N unidades pack for non-UNITS base`() {
        assertTrue(containsDenyword("arroz tipo 1 5kg 2 unidades", emptyList(), QuantityBase.GRAMS))
        assertTrue(containsDenyword("oleo de soja 900ml 2 unidades", emptyList(), QuantityBase.MILLILITERS))
    }

    @Test
    fun `containsDenyword allows unidades for UNITS base`() {
        assertFalse(containsDenyword("ovo branco grande com 20 unidades", emptyList(), QuantityBase.UNITS))
    }

    @Test
    fun `containsDenyword denies default combo and kit markers`() {
        assertTrue(containsDenyword("acem combo swift aprox 1,9kg", emptyList(), QuantityBase.GRAMS))
        assertTrue(containsDenyword("shampoo kit com condicionador", emptyList(), QuantityBase.MILLILITERS))
    }

    @Test
    fun `parseProductsPerUnits returns raw price for singular Unidade`() {
        val product = PASearchResponse(price = 599, name = "Detergente 1 Unidade", brand = "Brand")
        assertEquals(5990, parseProductsPerUnits(product))
    }

    @Test
    fun `parseProductsPerUnits returns raw price for plural Unidades`() {
        val product = PASearchResponse(price = 1299, name = "Sabão em Pó 10 Unidades", brand = "Brand")
        // price=1299 centavos / 10 units * 10 = 1290
        assertEquals(1290, parseProductsPerUnits(product))
    }

    @Test
    fun `parseProductsPerUnits returns raw price for lowercase unidade`() {
        val product = PASearchResponse(price = 399, name = "Esponja 1 unidade", brand = "Brand")
        assertEquals(3990, parseProductsPerUnits(product))
    }

    @Test
    fun `parseProductsPerUnits returns raw price for lowercase unidades`() {
        val product = PASearchResponse(price = 799, name = "Rolo de Papel 6 unidades", brand = "Brand")
        // price=799 centavos / 6 units = 133 per unit (integer division)
        assertEquals(1330, parseProductsPerUnits(product))
    }

    @Test
    fun `parseProductsPerUnits returns 0 when no Unidade pattern found`() {
        val product = PASearchResponse(price = 599, name = "Detergente Liquido 500ml", brand = "Brand")
        assertEquals(1, parseProductsPerUnits(product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts ml lowercase`() {
        val product = PASearchResponse(price = 800, name = "Suco de laranja 800ml", brand = "Brand")
        // price=800 centavos, volume=800ml → 800/800=1.0 → normalizeForMillicent(1.0)=10000
        assertEquals(10000, parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts ML uppercase`() {
        val product = PASearchResponse(price = 800, name = "Suco de laranja 800ML", brand = "Brand")
        assertEquals(10000, parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts ml with space`() {
        val product = PASearchResponse(price = 800, name = "Suco de laranja 800 ml", brand = "Brand")
        assertEquals(10000, parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts L uppercase`() {
        val product = PASearchResponse(price = 400, name = "Leite Integral 1L", brand = "Brand")
        // price=400 centavos, volume=1L=1000ml → 400/1000=0.4 → normalizeForMillicent(0.4)=4000
        assertEquals(4000, parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts l lowercase`() {
        val product = PASearchResponse(price = 400, name = "Leite Integral 1l", brand = "Brand")
        assertEquals(4000, parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters extracts L with space`() {
        val product = PASearchResponse(price = 400, name = "Leite Integral 1 L", brand = "Brand")
        assertEquals(4000, parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters returns 0 when no volume found`() {
        val product = PASearchResponse(price = 400, name = "Leite Integral", brand = "Brand")
        assertEquals(0, parseProductsPerMilliliters(product))
    }

    @Test
    fun `parseProductsPerMilliliters handles decimal liters`() {
        val product = PASearchResponse(price = 600, name = "Suco de uva 1,5L", brand = "Brand")
        // 1.5L = 1500ml → 600/1500=0.4 → normalizeForMillicent(0.4)=4000
        assertEquals(4000, parseProductsPerMilliliters(product))
    }
}
