package website.woodendoor.portfo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import website.woodendoor.portfo.ui.PortfoApp

@RunWith(RobolectricTestRunner::class)
class PortfoAppRobolectricTest {

    @Rule
    @JvmField
    val composeTestRule = createComposeRule()

    @Test
    fun appShell_rendersTopBarAndDefaultTaiwanMarket() {
        composeTestRule.setContent {
            PortfoApp()
        }

        // Verify TopAppBar title
        composeTestRule.onNodeWithText("Portfo").assertIsDisplayed()

        // Verify Market tabs
        composeTestRule.onNodeWithTag("tab_tw").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_us").assertIsDisplayed()

        // Verify initial view is Taiwan market container
        composeTestRule.onNodeWithTag("taiwan_market_container").assertIsDisplayed()
        composeTestRule.onNodeWithText("總投資成本").assertIsDisplayed()
    }

    @Test
    fun appShell_switchesMarketTabsCorrectly() {
        composeTestRule.setContent {
            PortfoApp()
        }

        // Verify Taiwan is initially displayed
        composeTestRule.onNodeWithTag("taiwan_market_container").assertIsDisplayed()

        // Click US tab
        composeTestRule.onNodeWithTag("tab_us").performClick()

        // Verify US market container is displayed
        composeTestRule.onNodeWithTag("us_market_container").assertIsDisplayed()
        composeTestRule.onNodeWithText("總投資成本").assertIsDisplayed()

        // Switch back to Taiwan tab
        composeTestRule.onNodeWithTag("tab_tw").performClick()
        composeTestRule.onNodeWithTag("taiwan_market_container").assertIsDisplayed()
        composeTestRule.onNodeWithText("總投資成本").assertIsDisplayed()
    }

    @Test
    fun appShell_togglesTheme() {
        composeTestRule.setContent {
            PortfoApp(systemDarkTheme = false)
        }

        // Initial toggle button should have content description for switching to dark theme
        composeTestRule.onNodeWithTag("theme_toggle_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_toggle_button").performClick()
        // Still displayed and functional after toggling
        composeTestRule.onNodeWithTag("theme_toggle_button").assertIsDisplayed()
    }
}
