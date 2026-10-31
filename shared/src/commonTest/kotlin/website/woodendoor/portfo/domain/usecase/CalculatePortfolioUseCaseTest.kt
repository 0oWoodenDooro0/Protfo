package website.woodendoor.portfo.domain.usecase

import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.domain.model.Transaction
import website.woodendoor.portfo.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalculatePortfolioUseCaseTest {

    private val useCase = CalculatePortfolioUseCase()

    @Test
    fun testSingleBuyLot() {
        val txs = listOf(
            Transaction(
                id = 1L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.BUY,
                price = 600.0,
                shares = 1000.0,
                fee = 100.0,
                transactedAt = 1000L
            )
        )

        val result = useCase(Market.TAIWAN, txs)

        assertEquals(1, result.holdings.size)
        val holding = result.holdings[0]
        assertEquals("2330", holding.symbol)
        assertEquals("台積電", holding.name)
        assertEquals(1000.0, holding.totalShares)
        assertEquals(600100.0, holding.totalCostBasis)
        assertEquals(600.10, holding.avgCostPerShare, 0.001)

        assertEquals(600100.0, result.summary.totalCostBasis)
        assertEquals(1, result.summary.activeHoldingCount)
        assertEquals(0.0, result.summary.realizedPnL)
    }

    @Test
    fun testMultipleBuyLotsWeightedAverageCost() {
        val txs = listOf(
            Transaction(
                id = 1L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.BUY,
                price = 600.0,
                shares = 1000.0,
                fee = 100.0,
                transactedAt = 1000L
            ),
            Transaction(
                id = 2L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.BUY,
                price = 900.0,
                shares = 500.0,
                fee = 50.0,
                transactedAt = 2000L
            ),
            Transaction(
                id = 3L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.BUY,
                price = 1000.0,
                shares = 100.0,
                fee = 20.0,
                transactedAt = 3000L
            )
        )

        val result = useCase(Market.TAIWAN, txs)

        assertEquals(1, result.holdings.size)
        val holding = result.holdings[0]
        assertEquals(1600.0, holding.totalShares)
        assertEquals(1150170.0, holding.totalCostBasis)
        assertEquals(718.85625, holding.avgCostPerShare, 0.0001)

        assertEquals(1150170.0, result.summary.totalCostBasis)
        assertEquals(1, result.summary.activeHoldingCount)
    }

    @Test
    fun testMultipleStocksAggregatedSummary() {
        val txs = listOf(
            Transaction(
                id = 1L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.BUY,
                price = 500.0,
                shares = 1000.0,
                fee = 50.0,
                transactedAt = 1000L
            ),
            Transaction(
                id = 2L,
                market = Market.TAIWAN,
                symbol = "2454",
                name = "聯發科",
                type = TransactionType.BUY,
                price = 1200.0,
                shares = 200.0,
                fee = 40.0,
                transactedAt = 2000L
            ),
            Transaction(
                id = 3L,
                market = Market.US,
                symbol = "AAPL",
                name = "Apple Inc.",
                type = TransactionType.BUY,
                price = 200.0,
                shares = 10.0,
                fee = 1.0,
                transactedAt = 3000L
            )
        )

        val twResult = useCase(Market.TAIWAN, txs)
        assertEquals(2, twResult.holdings.size)
        assertEquals(2, twResult.summary.activeHoldingCount)
        assertEquals(740090.0, twResult.summary.totalCostBasis)

        val usResult = useCase(Market.US, txs)
        assertEquals(1, usResult.holdings.size)
        assertEquals("AAPL", usResult.holdings[0].symbol)
        assertEquals(2001.0, usResult.summary.totalCostBasis)
    }

    @Test
    fun testPartialSellDeductsCostBasisAndComputesRealizedPnL() {
        val txs = listOf(
            Transaction(
                id = 1L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.BUY,
                price = 100.0,
                shares = 1000.0,
                fee = 0.0,
                transactedAt = 1000L
            ),
            Transaction(
                id = 2L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.SELL,
                price = 150.0,
                shares = 400.0,
                fee = 50.0,
                tax = 150.0,
                transactedAt = 2000L
            )
        )

        val result = useCase(Market.TAIWAN, txs)
        assertEquals(1, result.holdings.size)
        val holding = result.holdings[0]
        assertEquals(600.0, holding.totalShares)
        assertEquals(60000.0, holding.totalCostBasis)
        assertEquals(100.0, holding.avgCostPerShare)
        assertEquals(19800.0, holding.realizedPnL)
        assertEquals(19800.0, result.summary.realizedPnL)
    }

    @Test
    fun testFullLiquidationResetsPosition() {
        val txs = listOf(
            Transaction(
                id = 1L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.BUY,
                price = 100.0,
                shares = 1000.0,
                fee = 0.0,
                transactedAt = 1000L
            ),
            Transaction(
                id = 2L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.SELL,
                price = 120.0,
                shares = 1000.0,
                fee = 0.0,
                tax = 0.0,
                transactedAt = 2000L
            )
        )

        val result = useCase(Market.TAIWAN, txs)
        assertTrue(result.holdings.isEmpty())
        assertEquals(0, result.summary.activeHoldingCount)
        assertEquals(0.0, result.summary.totalCostBasis)
        assertEquals(20000.0, result.summary.realizedPnL)
    }

    @Test
    fun testUsFractionalShareBuySingleLot() {
        val txs = listOf(
            Transaction(
                id = 1L,
                market = Market.US,
                symbol = "AAPL",
                name = "Apple Inc.",
                type = TransactionType.BUY,
                price = 120.20,
                shares = 2.5,
                fee = 1.0,
                transactedAt = 1000L
            )
        )

        val result = useCase(Market.US, txs)
        assertEquals(1, result.holdings.size)
        val holding = result.holdings[0]
        assertEquals("AAPL", holding.symbol)
        assertEquals("Apple Inc.", holding.name)
        assertEquals(2.5, holding.totalShares)
        assertEquals(301.50, holding.totalCostBasis, 0.001)
        assertEquals(120.60, holding.avgCostPerShare, 0.001)

        assertEquals(301.50, result.summary.totalCostBasis, 0.001)
        assertEquals(1, result.summary.activeHoldingCount)
        assertEquals(0.0, result.summary.realizedPnL)
    }

    @Test
    fun testUsFractionalShareBuyMultipleLotsWeightedAverage() {
        val txs = listOf(
            Transaction(
                id = 1L,
                market = Market.US,
                symbol = "GOOGL",
                name = "Alphabet Inc.",
                type = TransactionType.BUY,
                price = 100.0,
                shares = 1.25,
                fee = 1.0,
                transactedAt = 1000L
            ),
            Transaction(
                id = 2L,
                market = Market.US,
                symbol = "GOOGL",
                name = "Alphabet Inc.",
                type = TransactionType.BUY,
                price = 200.0,
                shares = 0.75,
                fee = 1.0,
                transactedAt = 2000L
            )
        )

        val result = useCase(Market.US, txs)
        assertEquals(1, result.holdings.size)
        val holding = result.holdings[0]
        assertEquals(2.0, holding.totalShares)
        // Cost: (100*1.25 + 1) + (200*0.75 + 1) = 126.0 + 151.0 = 277.0
        assertEquals(277.0, holding.totalCostBasis, 0.001)
        // Avg cost: 277.0 / 2.0 = 138.50
        assertEquals(138.50, holding.avgCostPerShare, 0.001)
        assertEquals(277.0, result.summary.totalCostBasis, 0.001)
    }

    @Test
    fun testUsFractionalShareHighPrecisionEliminatesFloatingPointDrift() {
        val txs = listOf(
            Transaction(
                id = 1L,
                market = Market.US,
                symbol = "TSLA",
                name = "Tesla Inc.",
                type = TransactionType.BUY,
                price = 100.0,
                shares = 0.1,
                fee = 0.0,
                transactedAt = 1000L
            ),
            Transaction(
                id = 2L,
                market = Market.US,
                symbol = "TSLA",
                name = "Tesla Inc.",
                type = TransactionType.BUY,
                price = 100.0,
                shares = 0.2,
                fee = 0.0,
                transactedAt = 2000L
            )
        )

        val result = useCase(Market.US, txs)
        assertEquals(1, result.holdings.size)
        val holding = result.holdings[0]
        assertEquals(0.3, holding.totalShares)
        assertEquals(30.0, holding.totalCostBasis, 0.001)
        assertEquals(100.0, holding.avgCostPerShare, 0.001)
    }

    @Test
    fun testMultiBuySellSequenceWithPartialAndFullSells() {
        val txs = listOf(
            // Lot 1: Buy 1000 shares @ 100, fee 10 -> costBasis = 100010, avgCost = 100.01
            Transaction(
                id = 1L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.BUY,
                price = 100.0,
                shares = 1000.0,
                fee = 10.0,
                transactedAt = 1000L
            ),
            // Sell 400 shares @ 150, fee 20, tax 180
            // costOfSold = 100.01 * 400 = 40004
            // netProceeds = 150 * 400 - 20 - 180 = 59800
            // pnl1 = 59800 - 40004 = 19796
            // remainingShares = 600, remainingCostBasis = 60006
            Transaction(
                id = 2L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.SELL,
                price = 150.0,
                shares = 400.0,
                fee = 20.0,
                tax = 180.0,
                transactedAt = 2000L
            ),
            // Lot 2: Buy 400 shares @ 200, fee 14
            // newCostBasis = 60006 + 80014 = 140020
            // totalShares = 1000, avgCost = 140.02
            Transaction(
                id = 3L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.BUY,
                price = 200.0,
                shares = 400.0,
                fee = 14.0,
                transactedAt = 3000L
            ),
            // Sell 500 shares @ 250, fee 25, tax 375
            // costOfSold = 140.02 * 500 = 70010
            // netProceeds = 250 * 500 - 25 - 375 = 124600
            // pnl2 = 124600 - 70010 = 54590
            // remainingShares = 500, remainingCostBasis = 70010, avgCost = 140.02
            // totalRealizedPnL = 19796 + 54590 = 74386
            Transaction(
                id = 4L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.SELL,
                price = 250.0,
                shares = 500.0,
                fee = 25.0,
                tax = 375.0,
                transactedAt = 4000L
            )
        )

        val result = useCase(Market.TAIWAN, txs)
        assertEquals(1, result.holdings.size)
        val holding = result.holdings[0]
        assertEquals(500.0, holding.totalShares)
        assertEquals(70010.0, holding.totalCostBasis, 0.01)
        assertEquals(140.02, holding.avgCostPerShare, 0.01)
        assertEquals(74386.0, holding.realizedPnL, 0.01)

        assertEquals(70010.0, result.summary.totalCostBasis, 0.01)
        assertEquals(1, result.summary.activeHoldingCount)
        assertEquals(74386.0, result.summary.realizedPnL, 0.01)
    }

    @Test
    fun testUsFractionalMultiBuySellSequenceAndFullLiquidation() {
        val txs = listOf(
            // Buy 2.5 shares @ 100.0, fee 1.0 -> costBasis = 251.0, avgCost = 100.40
            Transaction(
                id = 1L,
                market = Market.US,
                symbol = "AAPL",
                name = "Apple Inc.",
                type = TransactionType.BUY,
                price = 100.0,
                shares = 2.5,
                fee = 1.0,
                transactedAt = 1000L
            ),
            // Sell 1.0 shares @ 150.0, fee 1.0
            // costOfSold = 100.40 * 1.0 = 100.40
            // netProceeds = 150.0 - 1.0 = 149.0
            // pnl1 = 149.0 - 100.40 = 48.60
            // remainingShares = 1.5, remainingCostBasis = 150.60, avgCost = 100.40
            Transaction(
                id = 2L,
                market = Market.US,
                symbol = "AAPL",
                name = "Apple Inc.",
                type = TransactionType.SELL,
                price = 150.0,
                shares = 1.0,
                fee = 1.0,
                transactedAt = 2000L
            ),
            // Liquidate remaining 1.5 shares @ 120.0, fee 1.0
            // costOfSold = 150.60
            // netProceeds = 120.0 * 1.5 - 1.0 = 179.0
            // pnl2 = 179.0 - 150.60 = 28.40
            // totalRealizedPnL = 48.60 + 28.40 = 77.00
            Transaction(
                id = 3L,
                market = Market.US,
                symbol = "AAPL",
                name = "Apple Inc.",
                type = TransactionType.SELL,
                price = 120.0,
                shares = 1.5,
                fee = 1.0,
                transactedAt = 3000L
            )
        )

        val result = useCase(Market.US, txs)
        assertTrue(result.holdings.isEmpty())
        assertEquals(0, result.summary.activeHoldingCount)
        assertEquals(0.0, result.summary.totalCostBasis)
        assertEquals(77.0, result.summary.realizedPnL, 0.001)
    }

    @Test
    fun testSellWithLossGeneratesNegativeRealizedPnL() {
        val txs = listOf(
            Transaction(
                id = 1L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.BUY,
                price = 100.0,
                shares = 100.0,
                fee = 0.0,
                transactedAt = 1000L
            ),
            Transaction(
                id = 2L,
                market = Market.TAIWAN,
                symbol = "2330",
                name = "台積電",
                type = TransactionType.SELL,
                price = 70.0,
                shares = 50.0,
                fee = 10.0,
                tax = 10.5,
                transactedAt = 2000L
            )
        )

        // costOfSold = 100.0 * 50 = 5000.0
        // netProceeds = 70.0 * 50 - 10.0 - 10.5 = 3500 - 20.5 = 3479.5
        // pnl = 3479.5 - 5000.0 = -1520.5
        val result = useCase(Market.TAIWAN, txs)
        assertEquals(1, result.holdings.size)
        val holding = result.holdings[0]
        assertEquals(50.0, holding.totalShares)
        assertEquals(5000.0, holding.totalCostBasis)
        assertEquals(100.0, holding.avgCostPerShare)
        assertEquals(-1520.5, holding.realizedPnL)
        assertEquals(-1520.5, result.summary.realizedPnL)
    }
}
