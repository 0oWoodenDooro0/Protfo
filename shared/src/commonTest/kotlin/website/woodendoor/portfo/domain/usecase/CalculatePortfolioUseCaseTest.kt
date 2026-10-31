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
}
