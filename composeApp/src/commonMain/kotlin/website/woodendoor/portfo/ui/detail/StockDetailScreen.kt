package website.woodendoor.portfo.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import website.woodendoor.portfo.domain.model.Holding
import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.domain.model.Transaction
import website.woodendoor.portfo.domain.model.TransactionType
import website.woodendoor.portfo.domain.repository.PortfolioRepository
import website.woodendoor.portfo.ui.transaction.EditTransactionDialog
import website.woodendoor.portfo.ui.util.NumberFormatHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    symbol: String,
    market: Market,
    repository: PortfolioRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Collect transactions for this asset
    val rawTransactions by repository.getTransactionsForAsset(market, symbol).collectAsState(initial = emptyList())
    val transactions = remember(rawTransactions) {
        rawTransactions.sortedWith(
            compareByDescending<Transaction> { it.transactedAt }.thenByDescending { it.id }
        )
    }

    // Collect holdings for this market
    val holdings by repository.getHoldings(market).collectAsState(initial = emptyList())
    val holding = remember(holdings, symbol) {
        holdings.find { it.symbol.equals(symbol, ignoreCase = true) }
    }

    // Resolving asset name from holding, transactions, or symbol
    val assetName = remember(holding, transactions, symbol) {
        holding?.name?.ifBlank { null }
            ?: transactions.firstOrNull { it.name.isNotBlank() }?.name
            ?: symbol
    }

    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    // If transactions become empty after having had transactions (i.e. last transaction was deleted), cleanly return to market view
    var hasReceivedTransactions by remember { mutableStateOf(false) }
    LaunchedEffect(transactions) {
        if (transactions.isNotEmpty()) {
            hasReceivedTransactions = true
        } else if (hasReceivedTransactions) {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("stock_detail_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = symbol,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("detail_title_symbol")
                        )
                        if (assetName.isNotBlank() && assetName != symbol) {
                            Text(
                                text = assetName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("detail_title_name")
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card: current holding summary for this asset
            StockHoldingHeaderCard(
                holding = holding,
                market = market
            )

            // Section title: Transaction History
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${transactions.size} records",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Transaction history list in reverse chronological order
            if (transactions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("detail_empty_transactions"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No transactions recorded for this asset",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("detail_transactions_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = transactions, key = { it.id }) { tx ->
                        TransactionHistoryCard(
                            transaction = tx,
                            market = market,
                            onEditClick = { editingTransaction = tx },
                            onDeleteClick = { transactionToDelete = tx }
                        )
                    }
                }
            }
        }

        // Edit Transaction Dialog
        if (editingTransaction != null) {
            EditTransactionDialog(
                transaction = editingTransaction!!,
                repository = repository,
                onDismiss = { editingTransaction = null },
                onSaveTransaction = { updatedTx ->
                    editingTransaction = null
                    coroutineScope.launch {
                        repository.updateTransaction(updatedTx)
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (transactionToDelete != null) {
            val txToDelete = transactionToDelete!!
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                modifier = Modifier.testTag("dialog_delete_transaction"),
                title = {
                    Text(
                        text = "Delete Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete this ${txToDelete.type.name.lowercase()} transaction for ${txToDelete.symbol}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val remainingCount = transactions.count { it.id != txToDelete.id }
                            val idToDelete = txToDelete.id
                            transactionToDelete = null
                            coroutineScope.launch {
                                repository.deleteTransaction(idToDelete)
                                if (remainingCount == 0) {
                                    onBack()
                                }
                            }
                        },
                        modifier = Modifier.testTag("button_confirm_delete")
                    ) {
                        Text(
                            text = "Confirm",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { transactionToDelete = null },
                        modifier = Modifier.testTag("button_cancel_delete")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Header card showing current holding summary for this asset:
 * total shares, weighted average cost, total cost basis.
 */
@Composable
fun StockHoldingHeaderCard(
    holding: Holding?,
    market: Market,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("detail_summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Holding Summary",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = market.currency,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val totalShares = holding?.totalShares ?: 0.0
                    val avgCost = holding?.avgCostPerShare ?: 0.0
                    val costBasis = holding?.totalCostBasis ?: 0.0
                    val isIntegerMarket = market == Market.TAIWAN

                    // Column 1: Total Shares
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detail_holding_shares")
                    ) {
                        Text(
                            text = "Total Shares",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = NumberFormatHelper.formatShares(totalShares, isIntegerMarket = isIntegerMarket),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Column 2: Weighted Average Cost
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detail_holding_avg_cost")
                    ) {
                        Text(
                            text = "Weighted Avg Cost",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val formattedAvgCost = if (market == Market.US) {
                            "$ ${NumberFormatHelper.formatDecimal(avgCost, 2)}"
                        } else {
                            NumberFormatHelper.formatDecimal(avgCost, 2)
                        }
                        Text(
                            text = formattedAvgCost,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Column 3: Total Cost Basis
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detail_holding_cost_basis")
                    ) {
                        Text(
                            text = "Total Cost Basis",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val formattedCostBasis = if (market == Market.US) {
                            "$ ${NumberFormatHelper.formatDecimal(costBasis, 2)}"
                        } else {
                            NumberFormatHelper.formatCurrency(costBasis, prefix = "NT$ ", decimals = 0)
                        }
                        Text(
                            text = formattedCostBasis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Realized PnL row if any realized profit or loss exists
            val realizedPnL = holding?.realizedPnL ?: 0.0
            if (realizedPnL > 1e-6 || realizedPnL < -1e-6) {
                val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                val pnlColor = when {
                    realizedPnL > 1e-6 -> if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                    realizedPnL < -1e-6 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val prefix = if (market == Market.US) "$ " else "NT$ "
                val formattedPnL = if (realizedPnL > 1e-6) {
                    "+${NumberFormatHelper.formatCurrency(realizedPnL, prefix = prefix, decimals = if (market == Market.US) 2 else 0)}"
                } else {
                    NumberFormatHelper.formatCurrency(realizedPnL, prefix = prefix, decimals = if (market == Market.US) 2 else 0)
                }
                Text(
                    text = "Realized PnL: $formattedPnL",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = pnlColor,
                    modifier = Modifier.testTag("detail_holding_realized_pnl")
                )
            }
        }
    }
}

/**
 * Transaction history item with direction indicator (buy vs sell) vector icon,
 * clear color accents, date, shares, price, fee, tax, and action buttons (Edit, Delete).
 */
@Composable
fun TransactionHistoryCard(
    transaction: Transaction,
    market: Market,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Direction indicator, Date, Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direction indicator with standard Material vector icon & badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isBuy = transaction.type == TransactionType.BUY
                    val badgeBg = if (isBuy) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                    val badgeContentColor = if (isBuy) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                    val directionIcon = if (isBuy) {
                        Icons.Default.ArrowDownward
                    } else {
                        Icons.Default.ArrowUpward
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg,
                        modifier = Modifier.testTag("tx_direction_badge_${transaction.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = directionIcon,
                                contentDescription = if (isBuy) "Buy" else "Sell",
                                modifier = Modifier.size(16.dp),
                                tint = badgeContentColor
                            )
                            Text(
                                text = if (isBuy) "Buy" else "Sell",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = badgeContentColor
                            )
                        }
                    }

                    Text(
                        text = NumberFormatHelper.formatDate(transaction.transactedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("tx_date_${transaction.id}")
                    )
                }

                // Action buttons: Edit and Delete
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("tx_edit_button_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Transaction",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("tx_delete_button_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Transaction",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Row 2: Shares, Price, Fee, and Tax
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedShares = NumberFormatHelper.formatShares(
                        transaction.shares,
                        isIntegerMarket = market == Market.TAIWAN
                    )
                    val formattedPrice = if (market == Market.US) {
                        "$ ${NumberFormatHelper.formatDecimal(transaction.price, 2)}"
                    } else {
                        NumberFormatHelper.formatDecimal(transaction.price, 2)
                    }

                    Text(
                        text = "$formattedShares @ $formattedPrice",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("tx_shares_price_${transaction.id}")
                    )

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        val feeStr = if (market == Market.US) {
                            "$ ${NumberFormatHelper.formatDecimal(transaction.fee, 2)}"
                        } else {
                            NumberFormatHelper.formatDecimal(transaction.fee, 0)
                        }
                        Text(
                            text = "Fee: $feeStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("tx_fee_${transaction.id}")
                        )
                        if (transaction.type == TransactionType.SELL) {
                            val taxStr = if (market == Market.US) {
                                "$ ${NumberFormatHelper.formatDecimal(transaction.tax, 2)}"
                            } else {
                                NumberFormatHelper.formatDecimal(transaction.tax, 0)
                            }
                            Text(
                                text = "Tax: $taxStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("tx_tax_${transaction.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}
