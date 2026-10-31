package website.woodendoor.portfo

import website.woodendoor.portfo.ui.util.NumberFormatHelper
import kotlin.test.Test
import kotlin.test.assertEquals

class NumberFormatHelperTest {

    @Test
    fun testFormatSharesIntegerMarket() {
        assertEquals("1,000 股", NumberFormatHelper.formatShares(1000.0, isIntegerMarket = true))
        assertEquals("0 股", NumberFormatHelper.formatShares(0.0, isIntegerMarket = true))
        assertEquals("1,234,567 股", NumberFormatHelper.formatShares(1234567.0, isIntegerMarket = true))
    }

    @Test
    fun testFormatSharesFractionalMarket() {
        // High precision and clean display formatting
        assertEquals("2.50 股", NumberFormatHelper.formatShares(2.5, isIntegerMarket = false))
        assertEquals("0.50 股", NumberFormatHelper.formatShares(0.5, isIntegerMarket = false))
        assertEquals("1.25 股", NumberFormatHelper.formatShares(1.25, isIntegerMarket = false))
        assertEquals("2.75 股", NumberFormatHelper.formatShares(2.75, isIntegerMarket = false))
        assertEquals("10.00 股", NumberFormatHelper.formatShares(10.0, isIntegerMarket = false))
        assertEquals("1.125 股", NumberFormatHelper.formatShares(1.125, isIntegerMarket = false))
    }

    @Test
    fun testFormatCurrencyTwdAndUsd() {
        assertEquals("NT$ 600,100", NumberFormatHelper.formatCurrency(600100.0, prefix = "NT$ ", decimals = 0))
        assertEquals("NT$ 0", NumberFormatHelper.formatCurrency(0.0, prefix = "NT$ ", decimals = 0))

        assertEquals("$ 301.50", NumberFormatHelper.formatCurrency(301.50, prefix = "$ ", decimals = 2))
        assertEquals("$ 120.60", NumberFormatHelper.formatCurrency(120.60, prefix = "$ ", decimals = 2))
        assertEquals("$ 0.00", NumberFormatHelper.formatCurrency(0.0, prefix = "$ ", decimals = 2))
    }

    @Test
    fun testFormatDecimal() {
        assertEquals("120.60", NumberFormatHelper.formatDecimal(120.60, 2))
        assertEquals("301.50", NumberFormatHelper.formatDecimal(301.50, 2))
        assertEquals("1,234.56", NumberFormatHelper.formatDecimal(1234.56, 2))
        assertEquals("0.00", NumberFormatHelper.formatDecimal(0.0, 2))
    }
}
