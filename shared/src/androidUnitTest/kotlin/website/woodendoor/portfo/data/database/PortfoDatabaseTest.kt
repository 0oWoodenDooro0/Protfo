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
}
