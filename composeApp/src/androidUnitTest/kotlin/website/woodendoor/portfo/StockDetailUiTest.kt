package website.woodendoor.portfo

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import website.woodendoor.portfo.data.database.PortfoDatabase
import website.woodendoor.portfo.data.database.createInMemoryPortfoDatabase
import website.woodendoor.portfo.data.repository.PortfolioRepositoryImpl
import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.domain.model.Transaction
import website.woodendoor.portfo.domain.model.TransactionType
import website.woodendoor.portfo.domain.repository.PortfolioRepository
import website.woodendoor.portfo.ui.PortfoApp

@RunWith(RobolectricTestRunner::class)
class StockDetailUiTest {

    @Rule
    @JvmField
    val composeTestRule = createComposeRule()

    private lateinit var database: PortfoDatabase
    private lateinit var repository: PortfolioRepository

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
    fun tapHoldingCard_opensStockDetailScreenAndNavigatesBack() {
        composeTestRule.setContent {
            PortfoApp(repository = repository)
        }

        // Add 2330 Buy transaction
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_name").performTextInput("台積電")
        composeTestRule.onNodeWithTag("input_price").performTextInput("600")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1000")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        // Wait for holding card to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("holding_card_2330").fetchSemanticsNodes().isNotEmpty()
        }

        // Tap holding card to drill down
        composeTestRule.onNodeWithTag("holding_card_2330").performClick()

        // Detail screen should be displayed
        composeTestRule.onNodeWithTag("stock_detail_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_title_symbol").assertTextContains("2330")
        composeTestRule.onNodeWithTag("detail_title_name").assertTextContains("台積電")
        composeTestRule.onNodeWithTag("detail_back_button").assertIsDisplayed()

        // Header card holding summary
        composeTestRule.onNodeWithTag("detail_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("持股數量").assertIsDisplayed()
        composeTestRule.onNodeWithText("1,000 股").assertIsDisplayed()
        composeTestRule.onNodeWithText("平均成本").assertIsDisplayed()
        composeTestRule.onNodeWithText("600").assertIsDisplayed()
        composeTestRule.onNodeWithText("總成本").assertIsDisplayed()
        composeTestRule.onNodeWithText("NT$ 600,000").assertIsDisplayed()

        // Transaction history
        composeTestRule.onNodeWithText("交易紀錄").assertIsDisplayed()
        composeTestRule.onNodeWithText("買進").assertIsDisplayed()
        composeTestRule.onNodeWithText("1,000 股 @ 600").assertIsDisplayed()
        composeTestRule.onNodeWithText("手續費: 0").assertIsDisplayed()

        // Tap back button
        composeTestRule.onNodeWithTag("detail_back_button").performClick()

        // Back to Taiwan market view
        composeTestRule.onNodeWithTag("taiwan_market_container").assertIsDisplayed()
    }

    @Test
    fun editTransactionFlow_updatesValuesAndRecalculatesReactively() {
        composeTestRule.setContent {
            PortfoApp(repository = repository)
        }

        // Add 2330 Buy: 1000 shares @ 600
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_name").performTextInput("台積電")
        composeTestRule.onNodeWithTag("input_price").performTextInput("600")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1000")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("holding_card_2330").fetchSemanticsNodes().isNotEmpty()
        }

        // Open detail screen
        composeTestRule.onNodeWithTag("holding_card_2330").performClick()
        composeTestRule.onNodeWithTag("stock_detail_screen").assertIsDisplayed()

        // Tap Edit button on transaction
        composeTestRule.onAllNodesWithTag("tx_edit_button_1").fetchSemanticsNodes()
        // Find by content description or tag
        composeTestRule.onNodeWithTag("tx_edit_button_1").performClick()

        // Edit dialog should appear preloaded
        composeTestRule.onNodeWithTag("dialog_edit_transaction").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dialog_edit_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_price").assertTextContains("600")
        composeTestRule.onNodeWithTag("input_shares").assertTextContains("1000")

        // Change price to 700
        composeTestRule.onNodeWithTag("input_price").performTextClearance()
        composeTestRule.onNodeWithTag("input_price").performTextInput("700")

        // Change shares to 1500
        composeTestRule.onNodeWithTag("input_shares").performTextClearance()
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1500")

        // Save
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        // Wait for reactive update in detail screen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("1,500 股").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify updated header card values
        composeTestRule.onNodeWithText("1,500 股").assertIsDisplayed()
        composeTestRule.onNodeWithText("700").assertIsDisplayed()
        composeTestRule.onNodeWithText("NT$ 1,050,000").assertIsDisplayed()

        // Verify updated transaction card text
        composeTestRule.onNodeWithText("1,500 股 @ 700").assertIsDisplayed()

        // Navigate back and verify market container reflected the change
        composeTestRule.onNodeWithTag("detail_back_button").performClick()
        composeTestRule.onNodeWithTag("taiwan_market_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("taiwan_total_cost_basis").assertTextContains("NT$ 1,050,000")
        composeTestRule.onNodeWithTag("holding_card_2330").assertTextContains("1,500 股")
    }

    @Test
    fun deleteTransactionFlow_withCancelAndConfirm() {
        composeTestRule.setContent {
            PortfoApp(repository = repository)
        }

        // Add Lot 1: 1000 shares @ 600
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_name").performTextInput("台積電")
        composeTestRule.onNodeWithTag("input_price").performTextInput("600")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1000")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("holding_card_2330").fetchSemanticsNodes().isNotEmpty()
        }

        // Add Lot 2: 500 shares @ 800
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_price").performTextInput("800")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("500")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("1,500 股").fetchSemanticsNodes().isNotEmpty()
        }

        // Open detail screen
        composeTestRule.onNodeWithTag("holding_card_2330").performClick()
        composeTestRule.onNodeWithTag("stock_detail_screen").assertIsDisplayed()

        // 2 transactions should be listed
        composeTestRule.onNodeWithTag("detail_tx_count").assertTextContains("2", substring = true)

        // Click delete on Lot 2 (tx id 2)
        composeTestRule.onNodeWithTag("tx_delete_button_2").performClick()

        // Confirmation dialog appears
        composeTestRule.onNodeWithTag("dialog_delete_transaction").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dialog_delete_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_confirm_delete").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_cancel_delete").assertIsDisplayed()

        // Cancel first
        composeTestRule.onNodeWithTag("button_cancel_delete").performClick()

        // Transaction should still be there (2 records)
        composeTestRule.onNodeWithTag("detail_tx_count").assertTextContains("2", substring = true)

        // Click delete again, and Confirm
        composeTestRule.onNodeWithTag("tx_delete_button_2").performClick()
        composeTestRule.onNodeWithTag("button_confirm_delete").performClick()

        // Wait for update: only 1 record remains, holding is 1,000 shares @ 600.00
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("detail_tx_count").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("detail_tx_count").assertTextContains("1", substring = true)
        composeTestRule.onNodeWithText("1,000 股").assertIsDisplayed()
        composeTestRule.onNodeWithText("600").assertIsDisplayed()
        composeTestRule.onNodeWithText("NT$ 600,000").assertIsDisplayed()
    }

    @Test
    fun deleteLastTransaction_cleanlyReturnsToMarketView() {
        composeTestRule.setContent {
            PortfoApp(repository = repository)
        }

        // Add 1 transaction
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_name").performTextInput("台積電")
        composeTestRule.onNodeWithTag("input_price").performTextInput("600")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1000")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("holding_card_2330").fetchSemanticsNodes().isNotEmpty()
        }

        // Open detail screen
        composeTestRule.onNodeWithTag("holding_card_2330").performClick()
        composeTestRule.onNodeWithTag("stock_detail_screen").assertIsDisplayed()

        // Delete the only transaction
        composeTestRule.onNodeWithTag("tx_delete_button_1").performClick()
        composeTestRule.onNodeWithTag("dialog_delete_transaction").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_confirm_delete").performClick()

        // Clean return to market view
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("taiwan_market_container").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("taiwan_market_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("taiwan_empty_holdings").assertIsDisplayed()
        composeTestRule.onNodeWithText("NT$ 0").assertIsDisplayed()
    }

    @Test
    fun usMarketHolding_drillDownEditAndDelete() {
        composeTestRule.setContent {
            PortfoApp(initialMarket = Market.US, repository = repository)
        }

        // Add US stock AAPL: 2.5 shares @ 150.0, default fee 1.0 -> cost basis 376.00, avg cost 150.40
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("AAPL")
        composeTestRule.onNodeWithTag("input_name").performTextInput("Apple Inc.")
        composeTestRule.onNodeWithTag("input_price").performTextInput("150.0")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("2.5")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("holding_card_AAPL").fetchSemanticsNodes().isNotEmpty()
        }

        // Tap holding card to drill down
        composeTestRule.onNodeWithTag("holding_card_AAPL").performClick()
        composeTestRule.onNodeWithTag("stock_detail_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_title_symbol").assertTextContains("AAPL")
        composeTestRule.onNodeWithTag("detail_title_name").assertTextContains("Apple Inc.")

        // US metrics
        composeTestRule.onNodeWithText("2.50 股").assertIsDisplayed()
        composeTestRule.onNodeWithText("$ 150.40").assertIsDisplayed()
        composeTestRule.onNodeWithText("$ 376").assertIsDisplayed()

        // Edit transaction: change price to 160.0, shares to 3.0
        composeTestRule.onNodeWithTag("tx_edit_button_1").performClick()
        composeTestRule.onNodeWithTag("dialog_edit_transaction").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_price").performTextClearance()
        composeTestRule.onNodeWithTag("input_price").performTextInput("160.0")
        composeTestRule.onNodeWithTag("input_shares").performTextClearance()
        composeTestRule.onNodeWithTag("input_shares").performTextInput("3.0")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        // 3.0 shares @ 160.0 + 1.0 fee = 481.00 cost basis, avg cost = 160.33
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("3.00 股").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("3.00 股").assertIsDisplayed()
        composeTestRule.onNodeWithText("$ 160.33").assertIsDisplayed()
        composeTestRule.onNodeWithText("$ 481").assertIsDisplayed()

        // Delete transaction -> returns cleanly to US market view
        composeTestRule.onNodeWithTag("tx_delete_button_1").performClick()
        composeTestRule.onNodeWithTag("button_confirm_delete").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("us_market_container").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("us_market_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("us_empty_holdings").assertIsDisplayed()
        composeTestRule.onNodeWithText("$ 0.00").assertIsDisplayed()
    }
}
