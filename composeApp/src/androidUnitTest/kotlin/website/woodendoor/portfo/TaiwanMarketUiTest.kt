package website.woodendoor.portfo

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
