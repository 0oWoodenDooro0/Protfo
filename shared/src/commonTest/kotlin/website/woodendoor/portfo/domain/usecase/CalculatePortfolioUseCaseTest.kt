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
        // Expected cost basis: 600*1000 + 100 + 900*500 + 50 + 1000*100 + 20 = 600100 + 450050 + 100020 = 1150170.0
        assertEquals(1150170.0, holding.totalCostBasis)
        // avgCost: 1150170 / 1600 = 718.85625
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
        // 500050.0 + 240040.0 = 740090.0
        assertEquals(740090.0, twResult.summary.totalCostBasis)

        // US result ignores TW stocks
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
        // Realized PnL: (150*400) - 50 - 150 - (100*400) = 60000 - 200 - 40000 = 19800.0
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
}
