package website.woodendoor.portfo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.ui.components.MarketSegmentedBar
import website.woodendoor.portfo.ui.components.PortfoTopBar
import website.woodendoor.portfo.ui.market.TaiwanMarketContainer
import website.woodendoor.portfo.ui.market.UsMarketContainer
import website.woodendoor.portfo.ui.theme.PortfoTheme

@Composable
fun PortfoApp(
    initialMarket: Market = Market.TAIWAN,
    systemDarkTheme: Boolean = isSystemInDarkTheme()
) {
    var isDarkTheme by remember { mutableStateOf(systemDarkTheme) }
    var selectedMarket by remember { mutableStateOf(initialMarket) }

    PortfoTheme(darkTheme = isDarkTheme) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("portfo_scaffold"),
            topBar = {
                PortfoTopBar(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                MarketSegmentedBar(
                    selectedMarket = selectedMarket,
                    onMarketSelected = { selectedMarket = it }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("market_content_area")
                ) {
                    when (selectedMarket) {
                        Market.TAIWAN -> TaiwanMarketContainer()
                        Market.US -> UsMarketContainer()
                    }
                }
            }
        }
    }
}
