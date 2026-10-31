package website.woodendoor.portfo

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
import website.woodendoor.portfo.domain.repository.PortfolioRepository
import website.woodendoor.portfo.ui.PortfoApp

@RunWith(RobolectricTestRunner::class)
class UsMarketUiTest {

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
    fun initialUsMarketView_showsEmptyStateAndZeroCost() {
        composeTestRule.setContent {
            PortfoApp(initialMarket = Market.US, repository = repository)
        }

        // Summary Card
        composeTestRule.onNodeWithTag("us_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("US Portfolio Cost (USD)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("us_total_cost_basis").assertIsDisplayed()
        composeTestRule.onNodeWithText("$ 0.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Holdings: 0").assertIsDisplayed()

        // Empty state
        composeTestRule.onNodeWithTag("us_empty_holdings").assertIsDisplayed()
        composeTestRule.onNodeWithText("No US holdings recorded").assertIsDisplayed()
    }

    @Test
    fun addUsTransactionDialog_validatesFractionalSharesAndDefaultFee() {
        composeTestRule.setContent {
            PortfoApp(initialMarket = Market.US, repository = repository)
        }

        // Open Dialog from FAB
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("dialog_add_transaction").assertIsDisplayed()

        // Save should be disabled initially
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsNotEnabled()

        // Fee should default to 1.0 for US market
        composeTestRule.onNodeWithTag("input_fee").performScrollTo().assertIsDisplayed()

        // Enter symbol
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("AAPL")
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsNotEnabled()

        // Enter price
        composeTestRule.onNodeWithTag("input_price").performTextInput("150.0")
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsNotEnabled()

        // Enter fractional shares (0.5)
        composeTestRule.onNodeWithTag("input_shares").performTextInput("0.5")

        // Save should now be enabled
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsEnabled()

        // Change to another fractional share (2.75)
        composeTestRule.onNodeWithTag("input_shares").performTextClearance()
        composeTestRule.onNodeWithTag("input_shares").performTextInput("2.75")
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsEnabled()

        // Dismiss dialog
        composeTestRule.onNodeWithTag("button_cancel_transaction").performClick()
    }

    @Test
    fun addUsFractionalBuyTransaction_updatesSummaryAndVariantBCard() {
        composeTestRule.setContent {
            PortfoApp(initialMarket = Market.US, repository = repository)
        }

        // Open Dialog
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("dialog_add_transaction").assertIsDisplayed()

        // Fill form: 2.5 shares @ $120.20 with default fee $1.0 -> cost basis $301.50, avg cost $120.60
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("AAPL")
        composeTestRule.onNodeWithTag("input_name").performTextInput("Apple Inc.")
        composeTestRule.onNodeWithTag("input_price").performTextInput("120.20")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("2.5")

        // Save
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        // Wait for holding card
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("holding_card_AAPL").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify summary card
        composeTestRule.onNodeWithTag("us_total_cost_basis").assertTextContains("$ 301.50")
        composeTestRule.onNodeWithText("Holdings: 1").assertIsDisplayed()

        // Verify Variant B holding card
        composeTestRule.onNodeWithTag("holding_card_AAPL").assertIsDisplayed()
        composeTestRule.onNodeWithText("AAPL").assertIsDisplayed()
        composeTestRule.onNodeWithText("USD").assertIsDisplayed()

        // Verify three-column metrics
        composeTestRule.onNodeWithTag("holding_shares_AAPL", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("2.50 股").assertIsDisplayed()

        composeTestRule.onNodeWithTag("holding_avg_cost_AAPL", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("$ 120.60").assertIsDisplayed()

        composeTestRule.onNodeWithTag("holding_cost_basis_AAPL", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("holding_card_AAPL").assertTextContains("$ 301.50")
    }

    @Test
    fun assetAutocomplete_suggestsAndAutoFillsNameOnSymbolEntry() {
        composeTestRule.setContent {
            PortfoApp(initialMarket = Market.US, repository = repository)
        }

        // Add first transaction to persist asset
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("dialog_add_transaction").assertIsDisplayed()

        composeTestRule.onNodeWithTag("input_symbol").performTextInput("AAPL")
        composeTestRule.onNodeWithTag("input_name").performTextInput("Apple Inc.")
        composeTestRule.onNodeWithTag("input_price").performTextInput("120.20")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("2.5")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("holding_card_AAPL").fetchSemanticsNodes().isNotEmpty()
        }

        // Open Dialog a second time
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("dialog_add_transaction").assertIsDisplayed()

        // Typing AAPL should automatically auto-fill the asset name
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("AAPL")

        // Check that Apple Inc. is auto-filled inside input_name
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("input_name").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("input_name").assertTextContains("Apple Inc.")

        // Enter second lot: 1.25 shares @ $100.0, fee 1.0
        composeTestRule.onNodeWithTag("input_price").performTextInput("100.0")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1.25")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        // Total cost basis = 301.50 + 126.00 = 427.50
        // Total shares = 2.5 + 1.25 = 3.75
        // Avg cost = 427.50 / 3.75 = 114.00
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("3.75 股").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("us_total_cost_basis").assertTextContains("$ 427.50")
        composeTestRule.onNodeWithText("3.75 股").assertIsDisplayed()
        composeTestRule.onNodeWithText("$ 114.00").assertIsDisplayed()
    }

    @Test
    fun usFractionalSell_validatesOversellAndRecordsRealizedPnL() {
        composeTestRule.setContent {
            PortfoApp(initialMarket = Market.US, repository = repository)
        }

        // Buy 2.5 shares of AAPL @ $100.0 with fee $1.0 -> cost basis $251.0, avgCost $100.40
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("AAPL")
        composeTestRule.onNodeWithTag("input_name").performTextInput("Apple Inc.")
        composeTestRule.onNodeWithTag("input_price").performTextInput("100.0")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("2.5")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("holding_card_AAPL").fetchSemanticsNodes().isNotEmpty()
        }

        // Open Sell dialog
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithText("Sell").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("AAPL")

        // Max sellable hint must show 2.50 股
        composeTestRule.onNodeWithTag("hint_max_sellable").assertIsDisplayed()
        composeTestRule.onNodeWithText("Max sellable: 2.50 股").assertIsDisplayed()

        composeTestRule.onNodeWithTag("input_price").performTextInput("150.0")

        // Try oversell (3.0 > 2.5)
        composeTestRule.onNodeWithTag("input_shares").performTextInput("3.0")
        composeTestRule.onNodeWithTag("error_oversell").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsNotEnabled()

        // Correct to 1.0 fractional share
        composeTestRule.onNodeWithTag("input_shares").performTextClearance()
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1.0")
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsEnabled()
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        // Wait for update
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("1.50 股").fetchSemanticsNodes().isNotEmpty()
        }

        // Remaining shares = 1.50 股, cost basis = $ 150.60
        composeTestRule.onNodeWithText("1.50 股").assertIsDisplayed()
        composeTestRule.onNodeWithTag("holding_card_AAPL").assertTextContains("$ 150.60")

        // Realized PnL: proceeds $149 - cost $100.40 = +$ 48.60
        composeTestRule.onNodeWithTag("us_realized_pnl").assertIsDisplayed()
        composeTestRule.onNodeWithText("Realized PnL: +$ 48.60").assertIsDisplayed()
    }
}
