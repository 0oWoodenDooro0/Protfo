package website.woodendoor.portfo.data.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import website.woodendoor.portfo.data.database.entity.AssetEntity
import website.woodendoor.portfo.data.database.entity.TransactionEntity
import website.woodendoor.portfo.data.repository.PortfolioRepositoryImpl
import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.domain.model.Transaction
import website.woodendoor.portfo.domain.model.TransactionType
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PortfoDatabaseTest {

    private lateinit var database: PortfoDatabase
    private lateinit var repository: PortfolioRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = createInMemoryPortfoDatabase(context)
        repository = PortfolioRepositoryImpl(
            transactionDao = database.transactionDao(),
            assetDao = database.assetDao()
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testTransactionDaoPersistenceAndAssetUpsert() = runTest {
        val txEntity = TransactionEntity(
            market = "TW",
            symbol = "2330",
            name = "台積電",
            type = "BUY",
            price = 580.0,
            shares = 1000.0,
            fee = 80.0,
            transactedAt = 1700000000L
        )

        val id = database.transactionDao().insertTransaction(txEntity)
        assertTrue(id > 0)

        val fetched = database.transactionDao().getTransactionById(id)
        assertNotNull(fetched)
        assertEquals("2330", fetched.symbol)
        assertEquals("台積電", fetched.name)
        assertEquals(580.0, fetched.price)
        assertEquals(1000.0, fetched.shares)
        assertEquals(80.0, fetched.fee)

        // Asset upsert
        val asset = AssetEntity(
            symbol = "2330",
            market = "TW",
            name = "台積電",
            currency = "TWD",
            updatedAt = 1700000000L
        )
        database.assetDao().upsertAsset(asset)

        val fetchedAsset = database.assetDao().getAsset("2330", "TW")
        assertNotNull(fetchedAsset)
        assertEquals("台積電", fetchedAsset.name)
    }

    @Test
    fun testRepositoryReactiveFlowEmissions() = runTest {
        // Initial state is empty
        val initialHoldings = repository.getHoldings(Market.TAIWAN).first()
        assertTrue(initialHoldings.isEmpty())

        val initialSummary = repository.getMarketSummary(Market.TAIWAN).first()
        assertEquals(0.0, initialSummary.totalCostBasis)
        assertEquals(0, initialSummary.activeHoldingCount)

        // Add first BUY lot
        val tx1 = Transaction(
            market = Market.TAIWAN,
            symbol = "2330",
            name = "台積電",
            type = TransactionType.BUY,
            price = 600.0,
            shares = 1000.0,
            fee = 100.0,
            transactedAt = 1000L
        )
        val id1 = repository.addTransaction(tx1)
        assertTrue(id1 > 0)

        // Reactive holding updated
        val holdingsAfterLot1 = repository.getHoldings(Market.TAIWAN).first()
        assertEquals(1, holdingsAfterLot1.size)
        assertEquals(1000.0, holdingsAfterLot1[0].totalShares)
        assertEquals(600100.0, holdingsAfterLot1[0].totalCostBasis)
        assertEquals(600.10, holdingsAfterLot1[0].avgCostPerShare, 0.001)

        val summaryAfterLot1 = repository.getMarketSummary(Market.TAIWAN).first()
        assertEquals(600100.0, summaryAfterLot1.totalCostBasis)
        assertEquals(1, summaryAfterLot1.activeHoldingCount)

        // Add second BUY lot for same stock
        val tx2 = Transaction(
            market = Market.TAIWAN,
            symbol = "2330",
            name = "台積電",
            type = TransactionType.BUY,
            price = 900.0,
            shares = 500.0,
            fee = 50.0,
            transactedAt = 2000L
        )
        val id2 = repository.addTransaction(tx2)
        assertTrue(id2 > 0)

        val holdingsAfterLot2 = repository.getHoldings(Market.TAIWAN).first()
        assertEquals(1, holdingsAfterLot2.size)
        assertEquals(1500.0, holdingsAfterLot2[0].totalShares)
        assertEquals(1050150.0, holdingsAfterLot2[0].totalCostBasis)
        assertEquals(700.10, holdingsAfterLot2[0].avgCostPerShare, 0.001)

        val summaryAfterLot2 = repository.getMarketSummary(Market.TAIWAN).first()
        assertEquals(1050150.0, summaryAfterLot2.totalCostBasis)
        assertEquals(1, summaryAfterLot2.activeHoldingCount)

        // Delete second transaction
        repository.deleteTransaction(id2)

        val holdingsAfterDelete = repository.getHoldings(Market.TAIWAN).first()
        assertEquals(1, holdingsAfterDelete.size)
        assertEquals(1000.0, holdingsAfterDelete[0].totalShares)
        assertEquals(600100.0, holdingsAfterDelete[0].totalCostBasis)
        assertEquals(600.10, holdingsAfterDelete[0].avgCostPerShare, 0.001)
    }

    @Test
    fun testAssetAutocompletionSearchAndRecentOrdering() = runTest {
        // Upsert 3 US assets with different updatedAt timestamps
        database.assetDao().upsertAsset(
            AssetEntity(symbol = "GOOGL", market = "US", name = "Alphabet Inc.", currency = "USD", updatedAt = 1000L)
        )
        database.assetDao().upsertAsset(
            AssetEntity(symbol = "AMZN", market = "US", name = "Amazon.com Inc.", currency = "USD", updatedAt = 2000L)
        )
        database.assetDao().upsertAsset(
            AssetEntity(symbol = "AAPL", market = "US", name = "Apple Inc.", currency = "USD", updatedAt = 3000L)
        )

        // Recent assets (ordered by updatedAt DESC)
        val recentAssets = repository.getAssets(Market.US).first()
        assertEquals(3, recentAssets.size)
        assertEquals("AAPL", recentAssets[0].symbol)
        assertEquals("AMZN", recentAssets[1].symbol)
        assertEquals("GOOGL", recentAssets[2].symbol)

        // Search by symbol prefix
        val searchAa = repository.searchAssets(Market.US, "AA")
        assertEquals(1, searchAa.size)
        assertEquals("AAPL", searchAa[0].symbol)

        val searchAm = repository.searchAssets(Market.US, "AM")
        assertEquals(1, searchAm.size)
        assertEquals("AMZN", searchAm[0].symbol)

        // Search by company name substring
        val searchApple = repository.searchAssets(Market.US, "Apple")
        assertEquals(1, searchApple.size)
        assertEquals("AAPL", searchApple[0].symbol)
        assertEquals("Apple Inc.", searchApple[0].name)

        // Get single asset
        val assetAapl = repository.getAsset(Market.US, "aapl")
        assertNotNull(assetAapl)
        assertEquals("Apple Inc.", assetAapl.name)
        assertEquals("USD", assetAapl.currency)
    }

    @Test
    fun testAddUsTransactionAutomaticallyUpsertsAsset() = runTest {
        val usTx = Transaction(
            market = Market.US,
            symbol = "MSFT",
            name = "Microsoft Corporation",
            type = TransactionType.BUY,
            price = 400.0,
            shares = 2.5,
            fee = 1.0,
            transactedAt = 5000L
        )

        repository.addTransaction(usTx)

        val asset = repository.getAsset(Market.US, "MSFT")
        assertNotNull(asset)
        assertEquals("Microsoft Corporation", asset.name)
        assertEquals(Market.US, asset.market)
        assertEquals("USD", asset.currency)

        val usAssets = repository.getAssets(Market.US).first()
        assertEquals(1, usAssets.size)
        assertEquals("MSFT", usAssets[0].symbol)
    }

    @Test
    fun testRepositoryUpdateTransactionReactiveRecalculation() = runTest {
        // Add Lot 1: 1000 shares @ 600, fee 100
        val tx1 = Transaction(
            market = Market.TAIWAN,
            symbol = "2330",
            name = "台積電",
            type = TransactionType.BUY,
            price = 600.0,
            shares = 1000.0,
            fee = 100.0,
            transactedAt = 1000L
        )
        val id1 = repository.addTransaction(tx1)

        // Add Lot 2: 500 shares @ 900, fee 50
        val tx2 = Transaction(
            market = Market.TAIWAN,
            symbol = "2330",
            name = "台積電",
            type = TransactionType.BUY,
            price = 900.0,
            shares = 500.0,
            fee = 50.0,
            transactedAt = 2000L
        )
        val id2 = repository.addTransaction(tx2)

        // Initial combined: 1500 shares, cost basis = 600100 + 450050 = 1050150, avgCost = 700.10
        val initialHoldings = repository.getHoldings(Market.TAIWAN).first()
        assertEquals(1, initialHoldings.size)
        assertEquals(1500.0, initialHoldings[0].totalShares)
        assertEquals(1050150.0, initialHoldings[0].totalCostBasis)
        assertEquals(700.10, initialHoldings[0].avgCostPerShare, 0.001)

        // Update Lot 2: price from 900 to 1000, fee from 50 to 100
        // New Lot 2 cost = 500 * 1000 + 100 = 500100
        // New total cost basis = 600100 + 500100 = 1100200
        // New avgCost = 1100200 / 1500 = 733.4667
        val updatedTx2 = tx2.copy(id = id2, price = 1000.0, fee = 100.0)
        repository.updateTransaction(updatedTx2)

        val updatedHoldings = repository.getHoldings(Market.TAIWAN).first()
        assertEquals(1, updatedHoldings.size)
        assertEquals(1500.0, updatedHoldings[0].totalShares)
        assertEquals(1100200.0, updatedHoldings[0].totalCostBasis)
        assertEquals(733.467, updatedHoldings[0].avgCostPerShare, 0.001)

        val updatedSummary = repository.getMarketSummary(Market.TAIWAN).first()
        assertEquals(1100200.0, updatedSummary.totalCostBasis)
        assertEquals(1, updatedSummary.activeHoldingCount)

        // Add Sell transaction: 500 shares @ 1200, fee 50, tax 180
        // Net proceeds = 500 * 1200 - 50 - 180 = 599770
        // Cost of sold = 733.466667 * 500 = 366733.3333
        // Realized PnL = 599770 - 366733.33 = 233036.67
        val sellTx = Transaction(
            market = Market.TAIWAN,
            symbol = "2330",
            name = "台積電",
            type = TransactionType.SELL,
            price = 1200.0,
            shares = 500.0,
            fee = 50.0,
            tax = 180.0,
            transactedAt = 3000L
        )
        val sellId = repository.addTransaction(sellTx)

        val holdingsAfterSell = repository.getHoldings(Market.TAIWAN).first()
        assertEquals(1000.0, holdingsAfterSell[0].totalShares)
        assertEquals(233036.67, holdingsAfterSell[0].realizedPnL, 0.01)

        // Update Sell transaction: price from 1200 to 1300, fee 100, tax 195
        // New net proceeds = 500 * 1300 - 100 - 195 = 649705
        // Cost of sold = 366733.3333
        // New Realized PnL = 649705 - 366733.33 = 282971.67
        val updatedSellTx = sellTx.copy(id = sellId, price = 1300.0, fee = 100.0, tax = 195.0)
        repository.updateTransaction(updatedSellTx)

        val holdingsAfterSellUpdate = repository.getHoldings(Market.TAIWAN).first()
        assertEquals(1000.0, holdingsAfterSellUpdate[0].totalShares)
        assertEquals(282971.67, holdingsAfterSellUpdate[0].realizedPnL, 0.01)

        val summaryAfterSellUpdate = repository.getMarketSummary(Market.TAIWAN).first()
        assertEquals(282971.67, summaryAfterSellUpdate.realizedPnL, 0.01)
    }

    @Test
    fun testRepositoryDeleteTransactionReactiveRecalculation() = runTest {
        // Add 2 Buy transactions and 1 Sell transaction
        val tx1 = Transaction(
            market = Market.TAIWAN,
            symbol = "2330",
            name = "台積電",
            type = TransactionType.BUY,
            price = 600.0,
            shares = 1000.0,
            fee = 0.0,
            transactedAt = 1000L
        )
        val id1 = repository.addTransaction(tx1)

        val tx2 = Transaction(
            market = Market.TAIWAN,
            symbol = "2330",
            name = "台積電",
            type = TransactionType.BUY,
            price = 800.0,
            shares = 500.0,
            fee = 0.0,
            transactedAt = 2000L
        )
        val id2 = repository.addTransaction(tx2)

        // Total buy: 1500 shares, cost = 1,000,000, avgCost = 666.6667
        val sellTx = Transaction(
            market = Market.TAIWAN,
            symbol = "2330",
            name = "台積電",
            type = TransactionType.SELL,
            price = 900.0,
            shares = 500.0,
            fee = 0.0,
            tax = 0.0,
            transactedAt = 3000L
        )
        val sellId = repository.addTransaction(sellTx)

        // Realized PnL: (900 - 666.6667) * 500 = +116666.67
        val holdingsWithSell = repository.getHoldings(Market.TAIWAN).first()
        assertEquals(1000.0, holdingsWithSell[0].totalShares)
        assertEquals(116666.67, holdingsWithSell[0].realizedPnL, 0.01)

        // Delete the Sell transaction -> holding shares should return to 1500 and realized PnL to 0
        repository.deleteTransaction(sellId)

        val holdingsAfterSellDeleted = repository.getHoldings(Market.TAIWAN).first()
        assertEquals(1500.0, holdingsAfterSellDeleted[0].totalShares)
        assertEquals(1000000.0, holdingsAfterSellDeleted[0].totalCostBasis)
        assertEquals(0.0, holdingsAfterSellDeleted[0].realizedPnL)

        // Delete Lot 2 -> only Lot 1 remains: 1000 shares @ 600
        repository.deleteTransaction(id2)

        val holdingsAfterLot2Deleted = repository.getHoldings(Market.TAIWAN).first()
        assertEquals(1, holdingsAfterLot2Deleted.size)
        assertEquals(1000.0, holdingsAfterLot2Deleted[0].totalShares)
        assertEquals(600000.0, holdingsAfterLot2Deleted[0].totalCostBasis)
        assertEquals(600.0, holdingsAfterLot2Deleted[0].avgCostPerShare)

        // Delete the final transaction (Lot 1) -> holding should be completely removed!
        repository.deleteTransaction(id1)

        val holdingsEmpty = repository.getHoldings(Market.TAIWAN).first()
        assertTrue(holdingsEmpty.isEmpty())

        val summaryEmpty = repository.getMarketSummary(Market.TAIWAN).first()
        assertEquals(0.0, summaryEmpty.totalCostBasis)
        assertEquals(0, summaryEmpty.activeHoldingCount)
    }

    @Test
    fun testRepositoryGetTransactionsForAsset() = runTest {
        val txTw1 = Transaction(
            market = Market.TAIWAN,
            symbol = "2330",
            name = "台積電",
            type = TransactionType.BUY,
            price = 600.0,
            shares = 1000.0,
            transactedAt = 1000L
        )
        val txTw2 = Transaction(
            market = Market.TAIWAN,
            symbol = "2317",
            name = "鴻海",
            type = TransactionType.BUY,
            price = 150.0,
            shares = 2000.0,
            transactedAt = 2000L
        )
        val txTw3 = Transaction(
            market = Market.TAIWAN,
            symbol = "2330",
            name = "台積電",
            type = TransactionType.SELL,
            price = 700.0,
            shares = 500.0,
            transactedAt = 3000L
        )

        repository.addTransaction(txTw1)
        repository.addTransaction(txTw2)
        repository.addTransaction(txTw3)

        val asset2330Txs = repository.getTransactionsForAsset(Market.TAIWAN, "2330").first()
        assertEquals(2, asset2330Txs.size)
        assertEquals(TransactionType.BUY, asset2330Txs[0].type)
        assertEquals(TransactionType.SELL, asset2330Txs[1].type)

        val asset2317Txs = repository.getTransactionsForAsset(Market.TAIWAN, "2317").first()
        assertEquals(1, asset2317Txs.size)
        assertEquals("2317", asset2317Txs[0].symbol)
    }
}
