package website.woodendoor.portfo

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import website.woodendoor.portfo.domain.repository.PortfolioRepository
import website.woodendoor.portfo.ui.PortfoApp

@RunWith(RobolectricTestRunner::class)
class TaiwanMarketUiTest {

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
        // In-memory Room database lifecycle managed by test runner
    }

    @Test
    fun initialTaiwanMarketView_showsEmptyStateAndZeroCost() {
        composeTestRule.setContent {
            PortfoApp(repository = repository)
        }

        // Summary Card
        composeTestRule.onNodeWithTag("taiwan_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Taiwan Portfolio Cost (TWD)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("taiwan_total_cost_basis").assertIsDisplayed()
        composeTestRule.onNodeWithText("NT$ 0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Holdings: 0").assertIsDisplayed()

        // Empty state
        composeTestRule.onNodeWithTag("taiwan_empty_holdings").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Taiwan holdings recorded").assertIsDisplayed()
    }

    @Test
    fun addTransactionDialog_validatesInputProperly() {
        composeTestRule.setContent {
            PortfoApp(repository = repository)
        }

        // Open Dialog from FAB
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("dialog_add_transaction").assertIsDisplayed()

        // Save should be disabled initially
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsNotEnabled()

        // Enter symbol only -> still disabled
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsNotEnabled()

        // Enter price
        composeTestRule.onNodeWithTag("input_price").performTextInput("600")
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsNotEnabled()

        // Enter valid integer shares
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1000")

        // Now form should be valid and Save enabled
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsEnabled()

        // Dismiss dialog
        composeTestRule.onNodeWithTag("button_cancel_transaction").performClick()
    }

    @Test
    fun addBuyTransaction_updatesSummaryAndVariantBCard() {
        composeTestRule.setContent {
            PortfoApp(repository = repository)
        }

        // Open Dialog from FAB
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("dialog_add_transaction").assertIsDisplayed()

        // Fill form
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_name").performTextInput("台積電")
        composeTestRule.onNodeWithTag("input_price").performTextInput("600")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1000")
        composeTestRule.onNodeWithTag("input_fee").performTextClearance()
        composeTestRule.onNodeWithTag("input_fee").performTextInput("100")

        // Save
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        // Wait asynchronously for reactive Room Flow to emit updated values
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("holding_card_2330").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify summary card update
        composeTestRule.onNodeWithText("NT$ 600,100").assertIsDisplayed()
        composeTestRule.onNodeWithText("Holdings: 1").assertIsDisplayed()

        // Verify Variant B holding card
        composeTestRule.onNodeWithTag("holding_card_2330").assertIsDisplayed()
        composeTestRule.onNodeWithText("2330").assertIsDisplayed()
        composeTestRule.onNodeWithText("台積電").assertIsDisplayed()
        composeTestRule.onNodeWithText("TWD").assertIsDisplayed()

        // Metrics row
        composeTestRule.onNodeWithTag("holding_shares_2330", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("1,000 股").assertIsDisplayed()

        composeTestRule.onNodeWithTag("holding_avg_cost_2330", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("600.10").assertIsDisplayed()

        composeTestRule.onNodeWithTag("holding_cost_basis_2330", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("600,100").assertIsDisplayed()
    }

    @Test
    fun multipleBuyTransactions_calculatesWeightedAverageCostInUI() {
        composeTestRule.setContent {
            PortfoApp(repository = repository)
        }

        // Add Lot 1: 1000 shares @ 600, fee 100
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("dialog_add_transaction").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_name").performTextInput("台積電")
        composeTestRule.onNodeWithTag("input_price").performTextInput("600")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1000")
        composeTestRule.onNodeWithTag("input_fee").performTextClearance()
        composeTestRule.onNodeWithTag("input_fee").performTextInput("100")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        // Wait for Lot 1 to be reflected
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("NT$ 600,100").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("NT$ 600,100").assertIsDisplayed()

        // Add Lot 2: 500 shares @ 900, fee 50
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("dialog_add_transaction").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_name").performTextInput("台積電")
        composeTestRule.onNodeWithTag("input_price").performTextInput("900")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("500")
        composeTestRule.onNodeWithTag("input_fee").performTextClearance()
        composeTestRule.onNodeWithTag("input_fee").performTextInput("50")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        // Expected total cost basis = 600100 + 450050 = 1050150
        // Expected total shares = 1500
        // Expected avg cost = 1050150 / 1500 = 700.10
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("NT$ 1,050,150").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("NT$ 1,050,150").assertIsDisplayed()
        composeTestRule.onNodeWithText("Holdings: 1").assertIsDisplayed()

        composeTestRule.onNodeWithText("1,500 股").assertIsDisplayed()
        composeTestRule.onNodeWithText("700.10").assertIsDisplayed()
        composeTestRule.onNodeWithText("1,050,150").assertIsDisplayed()
    }

    @Test
    fun sellProtection_blocksSellingWhenUserDoesNotOwnStock() {
        composeTestRule.setContent {
            PortfoApp(repository = repository)
        }

        // Open Dialog
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("dialog_add_transaction").assertIsDisplayed()

        // Switch to Sell
        composeTestRule.onNodeWithText("Sell").performClick()

        // Enter symbol not owned
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")

        // Validation guard: error shown right below symbol
        composeTestRule.onNodeWithTag("error_no_shares_owned").assertIsDisplayed()
        composeTestRule.onNodeWithText("No shares owned for this asset").assertIsDisplayed()

        composeTestRule.onNodeWithTag("input_price").performTextInput("600")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1000")

        // Validation guard: save button remains disabled
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("button_cancel_transaction").performClick()
    }

    @Test
    fun sellProtection_blocksOversellingHeldShares() {
        composeTestRule.setContent {
            PortfoApp(repository = repository)
        }

        // Buy 1000 shares of 2330 first
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_name").performTextInput("台積電")
        composeTestRule.onNodeWithTag("input_price").performTextInput("600")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1000")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("holding_card_2330").fetchSemanticsNodes().isNotEmpty()
        }

        // Open Dialog to Sell
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithText("Sell").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")

        // Max sellable hint must be displayed
        composeTestRule.onNodeWithTag("hint_max_sellable").assertIsDisplayed()
        composeTestRule.onNodeWithText("Max sellable: 1,000 股").assertIsDisplayed()

        composeTestRule.onNodeWithTag("input_price").performTextInput("650")

        // Attempt to oversell: enter 1500 shares (> 1000)
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1500")

        // Oversell error displayed and save disabled
        composeTestRule.onNodeWithTag("error_oversell").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsNotEnabled()

        // Correct to valid 400 shares
        composeTestRule.onNodeWithTag("input_shares").performTextClearance()
        composeTestRule.onNodeWithTag("input_shares").performTextInput("400")

        // Save button must now be enabled
        composeTestRule.onNodeWithTag("button_save_transaction").assertIsEnabled()

        composeTestRule.onNodeWithTag("button_cancel_transaction").performClick()
    }

    @Test
    fun recordSellTransaction_updatesHoldingSharesAndRealizedPnL() {
        composeTestRule.setContent {
            PortfoApp(repository = repository)
        }

        // Buy Lot: 1000 shares @ 100, fee 0
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_name").performTextInput("台積電")
        composeTestRule.onNodeWithTag("input_price").performTextInput("100")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1000")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("holding_card_2330").fetchSemanticsNodes().isNotEmpty()
        }

        // Sell: 400 shares @ 150, fee 50, tax 150
        // Net proceeds = 60,000 - 50 - 150 = 59,800
        // Cost of sold = 100 * 400 = 40,000
        // Realized PnL = 59,800 - 40,000 = +19,800
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithText("Sell").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_price").performTextInput("150")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("400")
        composeTestRule.onNodeWithTag("input_fee").performTextClearance()
        composeTestRule.onNodeWithTag("input_fee").performTextInput("50")
        composeTestRule.onNodeWithTag("input_tax").performTextClearance()
        composeTestRule.onNodeWithTag("input_tax").performTextInput("150")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        // Wait for update
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("600 股").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify updated remaining shares and cost basis
        composeTestRule.onNodeWithText("600 股").assertIsDisplayed()
        composeTestRule.onNodeWithText("60,000").assertIsDisplayed()
        composeTestRule.onNodeWithText("NT$ 60,000").assertIsDisplayed()

        // Verify summary Realized PnL formatting and display
        composeTestRule.onNodeWithTag("taiwan_realized_pnl").assertIsDisplayed()
        composeTestRule.onNodeWithText("Realized PnL: +NT$ 19,800").assertIsDisplayed()
    }

    @Test
    fun fullLiquidation_removesHoldingAndAccumulatesRealizedPnL() {
        composeTestRule.setContent {
            PortfoApp(repository = repository)
        }

        // Buy 500 shares @ 100, fee 0
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_name").performTextInput("台積電")
        composeTestRule.onNodeWithTag("input_price").performTextInput("100")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("500")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("holding_card_2330").fetchSemanticsNodes().isNotEmpty()
        }

        // Liquidate all 500 shares @ 120, fee 0, tax 0
        // Realized PnL = (120 - 100) * 500 = +10,000
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithText("Sell").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_price").performTextInput("120")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("500")
        composeTestRule.onNodeWithTag("input_tax").performTextClearance()
        composeTestRule.onNodeWithTag("input_tax").performTextInput("0")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        // Holding should disappear and empty state shown
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("taiwan_empty_holdings").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("taiwan_empty_holdings").assertIsDisplayed()
        composeTestRule.onNodeWithText("NT$ 0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Holdings: 0").assertIsDisplayed()

        // Realized PnL is retained in market summary
        composeTestRule.onNodeWithTag("taiwan_realized_pnl").assertIsDisplayed()
        composeTestRule.onNodeWithText("Realized PnL: +NT$ 10,000").assertIsDisplayed()
    }

    @Test
    fun sellTransaction_autoCalculatesTaiwanSecuritiesTax() {
        composeTestRule.setContent {
            PortfoApp(repository = repository)
        }

        // Buy 1000 shares @ 100
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_name").performTextInput("台積電")
        composeTestRule.onNodeWithTag("input_price").performTextInput("100")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("1000")
        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("holding_card_2330").fetchSemanticsNodes().isNotEmpty()
        }

        // Sell: 500 shares @ 200 -> trade amount 100,000 -> 0.3% tax = 300
        composeTestRule.onNodeWithTag("fab_add_transaction").performClick()
        composeTestRule.onNodeWithText("Sell").performClick()
        composeTestRule.onNodeWithTag("input_symbol").performTextInput("2330")
        composeTestRule.onNodeWithTag("input_price").performTextInput("200")
        composeTestRule.onNodeWithTag("input_shares").performTextInput("500")

        // Check tax field was auto-calculated to 300
        composeTestRule.onNodeWithTag("input_tax").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_tax").assertTextContains("300")

        composeTestRule.onNodeWithTag("button_save_transaction").performClick()

        // Wait for update
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("500 股").fetchSemanticsNodes().isNotEmpty()
        }

        // Net proceeds = 100,000 - 300 = 99,700
        // Cost of sold = 100 * 500 = 50,000
        // Realized PnL = 99,700 - 50,000 = +49,700
        composeTestRule.onNodeWithTag("taiwan_realized_pnl").assertIsDisplayed()
        composeTestRule.onNodeWithText("Realized PnL: +NT$ 49,700").assertIsDisplayed()
    }
}
