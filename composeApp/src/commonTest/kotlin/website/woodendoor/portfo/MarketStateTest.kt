package website.woodendoor.portfo

import website.woodendoor.portfo.domain.model.Market
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MarketStateTest {

    @Test
    fun testMarketDefaultsAndSwitching() {
        var currentMarket = Market.TAIWAN
        assertEquals(Market.TAIWAN, currentMarket)
        assertEquals("TW", currentMarket.code)
        assertEquals("TWD", currentMarket.currency)
        assertEquals("Taiwan", currentMarket.displayName)

        // Switch to US market
        currentMarket = Market.US
        assertEquals(Market.US, currentMarket)
        assertEquals("US", currentMarket.code)
        assertEquals("USD", currentMarket.currency)
        assertEquals("US", currentMarket.displayName)

        // Verify distinct markets
        assertNotEquals(Market.TAIWAN, Market.US)
    }

    @Test
    fun testAllMarketsHaveValidProperties() {
        val markets = Market.entries
        assertEquals(2, markets.size)
        for (m in markets) {
            assertEquals(2, m.code.length)
            assertEquals(3, m.currency.length)
            assert(m.displayName.isNotBlank())
        }
    }
}
