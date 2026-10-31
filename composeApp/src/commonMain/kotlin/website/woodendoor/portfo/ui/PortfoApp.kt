package website.woodendoor.portfo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.domain.model.MarketSummary
import website.woodendoor.portfo.domain.repository.PortfolioRepository
import website.woodendoor.portfo.ui.components.MarketSegmentedBar
import website.woodendoor.portfo.ui.components.PortfoTopBar
import website.woodendoor.portfo.ui.detail.StockDetailScreen
import website.woodendoor.portfo.ui.market.TaiwanMarketContainer
import website.woodendoor.portfo.ui.market.UsMarketContainer
import website.woodendoor.portfo.ui.theme.PortfoTheme
import website.woodendoor.portfo.ui.transaction.AddTransactionDialog

@Composable
fun PortfoApp(
    initialMarket: Market = Market.TAIWAN,
    systemDarkTheme: Boolean = isSystemInDarkTheme(),
    repository: PortfolioRepository = rememberDefaultRepository()
) {
    var isDarkTheme by remember { mutableStateOf(systemDarkTheme) }
    var selectedMarket by remember { mutableStateOf(initialMarket) }
    var selectedHoldingSymbol by remember { mutableStateOf<String?>(null) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val taiwanHoldings by repository.getHoldings(Market.TAIWAN).collectAsState(initial = emptyList())
    val taiwanSummary by repository.getMarketSummary(Market.TAIWAN)
        .collectAsState(initial = MarketSummary(market = Market.TAIWAN))

    val usHoldings by repository.getHoldings(Market.US).collectAsState(initial = emptyList())
    val usSummary by repository.getMarketSummary(Market.US)
        .collectAsState(initial = MarketSummary(market = Market.US))

    PortfoTheme(darkTheme = isDarkTheme) {
        if (selectedHoldingSymbol != null) {
            StockDetailScreen(
                symbol = selectedHoldingSymbol!!,
                market = selectedMarket,
                repository = repository,
                onBack = { selectedHoldingSymbol = null }
            )
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("portfo_scaffold"),
                topBar = {
                    PortfoTopBar(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddTransactionDialog = true },
                        modifier = Modifier.testTag("fab_add_transaction")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Transaction"
                        )
                    }
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
                            Market.TAIWAN -> TaiwanMarketContainer(
                                summary = taiwanSummary,
                                holdings = taiwanHoldings,
                                onAddTransactionClick = { showAddTransactionDialog = true },
                                onHoldingClick = { holding -> selectedHoldingSymbol = holding.symbol }
                            )
                            Market.US -> UsMarketContainer(
                                summary = usSummary,
                                holdings = usHoldings,
                                onAddTransactionClick = { showAddTransactionDialog = true },
                                onHoldingClick = { holding -> selectedHoldingSymbol = holding.symbol }
                            )
                        }
                    }
                }

                if (showAddTransactionDialog) {
                    AddTransactionDialog(
                        initialMarket = selectedMarket,
                        repository = repository,
                        onDismiss = { showAddTransactionDialog = false },
                        onSaveTransaction = { transaction ->
                            showAddTransactionDialog = false
                            coroutineScope.launch {
                                repository.addTransaction(transaction)
                            }
                        }
                    )
                }
            }
        }
    }
}
