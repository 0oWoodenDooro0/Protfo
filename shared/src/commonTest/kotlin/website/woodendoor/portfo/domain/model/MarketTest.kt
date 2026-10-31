package website.woodendoor.portfo.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MarketTest {

    @Test
    fun testMarketProperties() {
        assertEquals("TW", Market.TAIWAN.code)
        assertEquals("TWD", Market.TAIWAN.currency)
        assertEquals("Taiwan", Market.TAIWAN.displayName)

        assertEquals("US", Market.US.code)
        assertEquals("USD", Market.US.currency)
        assertEquals("US", Market.US.displayName)
    }
}
