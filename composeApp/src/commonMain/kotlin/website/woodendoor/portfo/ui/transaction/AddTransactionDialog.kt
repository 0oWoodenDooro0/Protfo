package website.woodendoor.portfo.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.time.Clock
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.domain.model.Transaction
import website.woodendoor.portfo.domain.model.TransactionType
import website.woodendoor.portfo.domain.repository.PortfolioRepository
import website.woodendoor.portfo.ui.util.NumberFormatHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    initialMarket: Market = Market.TAIWAN,
    initialType: TransactionType = TransactionType.BUY,
    repository: PortfolioRepository? = null,
    onDismiss: () -> Unit,
    onSaveTransaction: (Transaction) -> Unit
) {
    var selectedMarket by remember { mutableStateOf(initialMarket) }
    var transactionType by remember { mutableStateOf(initialType) }
    var symbol by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isNameAutofilled by remember { mutableStateOf(false) }
    var priceText by remember { mutableStateOf("") }
    var sharesText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf(if (initialMarket == Market.US) "1.0" else "0") }
    var taxText by remember { mutableStateOf("0") }
    var isTaxManuallyEdited by remember { mutableStateOf(false) }
    var dateText by remember { mutableStateOf(getTodayDateString()) }

    // Collect saved assets for autocomplete
    val savedAssetsFlow = remember(selectedMarket, repository) {
        repository?.getAssets(selectedMarket) ?: flowOf(emptyList())
    }
    val savedAssets by savedAssetsFlow.collectAsState(initial = emptyList())

    // Collect active holdings to check ownership and max sellable shares
    val holdingsFlow = remember(selectedMarket, repository) {
        repository?.getHoldings(selectedMarket) ?: flowOf(emptyList())
    }
    val currentHoldings by holdingsFlow.collectAsState(initial = emptyList())

    val trimmedSymbol = symbol.trim()
    val matchingHolding = currentHoldings.find { it.symbol.equals(trimmedSymbol, ignoreCase = true) }
    val currentHoldingShares = matchingHolding?.totalShares ?: 0.0
    val ownsShares = matchingHolding != null && currentHoldingShares > 0.0

    val parsedPrice = priceText.toDoubleOrNull()
    val isPriceValid = parsedPrice != null && parsedPrice > 0.0

    val parsedShares = if (selectedMarket == Market.TAIWAN) {
        sharesText.toLongOrNull()?.toDouble()
    } else {
        sharesText.toDoubleOrNull()
    }

    val isOverselling = transactionType == TransactionType.SELL && parsedShares != null && (parsedShares > currentHoldingShares || !ownsShares)

    val isSharesValid = if (transactionType == TransactionType.SELL) {
        parsedShares != null && parsedShares > 0.0 && ownsShares && parsedShares <= currentHoldingShares
    } else {
        parsedShares != null && parsedShares > 0.0
    }

    val defaultFee = if (selectedMarket == Market.US) 1.0 else 0.0
    val parsedFee = feeText.toDoubleOrNull() ?: defaultFee
    val isFeeValid = feeText.isBlank() || (parsedFee >= 0.0)

    val parsedTax = taxText.toDoubleOrNull() ?: 0.0
    val isTaxValid = taxText.isBlank() || (parsedTax >= 0.0)

    val isFormValid = symbol.isNotBlank() &&
        (transactionType == TransactionType.BUY || ownsShares) &&
        isPriceValid &&
        isSharesValid &&
        isFeeValid &&
        (transactionType == TransactionType.BUY || isTaxValid)

    // Auto-calculate Taiwan securities transaction tax (0.3%) for Sell transactions
    LaunchedEffect(transactionType, selectedMarket, parsedPrice, parsedShares, isTaxManuallyEdited) {
        if (transactionType == TransactionType.SELL && selectedMarket == Market.TAIWAN && !isTaxManuallyEdited) {
            if (parsedPrice != null && parsedShares != null && parsedPrice > 0.0 && parsedShares > 0.0) {
                val estimatedTax = floor(parsedPrice * parsedShares * 0.003).toLong()
                taxText = estimatedTax.toString()
            }
        }
    }

    // Filter autocomplete suggestions
    val symbolSuggestions = if (trimmedSymbol.isNotEmpty()) {
        savedAssets.filter { asset ->
            asset.symbol.contains(trimmedSymbol, ignoreCase = true) &&
                !asset.symbol.equals(trimmedSymbol, ignoreCase = true)
        }.take(5)
    } else emptyList()

    val trimmedName = name.trim()
    val nameSuggestions = if (trimmedName.isNotEmpty() && !isNameAutofilled) {
        savedAssets.filter { asset ->
            asset.name.contains(trimmedName, ignoreCase = true) &&
                !asset.name.equals(trimmedName, ignoreCase = true)
        }.take(5)
    } else emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_add_transaction"),
        title = {
            Text(
                text = if (transactionType == TransactionType.BUY) "Record Buy Transaction" else "Record Sell Transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Market selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_market_selector"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMarket == Market.TAIWAN,
                        onClick = {
                            selectedMarket = Market.TAIWAN
                            if (feeText == "1.0" || feeText == "1" || feeText.isBlank()) {
                                feeText = "0"
                            }
                        },
                        label = { Text("Taiwan (TWD)") },
                        modifier = Modifier.testTag("input_market_tw")
                    )
                    FilterChip(
                        selected = selectedMarket == Market.US,
                        onClick = {
                            selectedMarket = Market.US
                            if (feeText == "0" || feeText.isBlank()) {
                                feeText = "1.0"
                            }
                            if (!isTaxManuallyEdited) {
                                taxText = "0"
                            }
                        },
                        label = { Text("US (USD)") },
                        modifier = Modifier.testTag("input_market_us")
                    )
                }

                // Transaction Type Selector (SegmentedButton)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_transaction_type_selector")
                ) {
                    SegmentedButton(
                        selected = transactionType == TransactionType.BUY,
                        onClick = { transactionType = TransactionType.BUY },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("button_type_buy"),
                        icon = {}
                    ) {
                        Text("Buy", modifier = Modifier.testTag("input_type_buy"))
                    }
                    SegmentedButton(
                        selected = transactionType == TransactionType.SELL,
                        onClick = {
                            transactionType = TransactionType.SELL
                            if (selectedMarket == Market.TAIWAN && !isTaxManuallyEdited && parsedPrice != null && parsedShares != null) {
                                val estimatedTax = floor(parsedPrice * parsedShares * 0.003).toLong()
                                taxText = estimatedTax.toString()
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("button_type_sell"),
                        icon = {}
                    ) {
                        Text("Sell", modifier = Modifier.testTag("input_type_sell"))
                    }
                }

                // Chips for quick picking assets
                if (symbol.isBlank()) {
                    if (transactionType == TransactionType.BUY && savedAssets.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recent_assets_chips"),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            savedAssets.take(3).forEach { asset ->
                                AssistChip(
                                    onClick = {
                                        symbol = asset.symbol
                                        name = asset.name
                                        isNameAutofilled = true
                                    },
                                    label = { Text(asset.symbol) },
                                    modifier = Modifier.testTag("chip_recent_${asset.symbol}")
                                )
                            }
                        }
                    } else if (transactionType == TransactionType.SELL) {
                        if (currentHoldings.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("holding_assets_chips"),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currentHoldings.take(3).forEach { holding ->
                                    AssistChip(
                                        onClick = {
                                            symbol = holding.symbol
                                            name = holding.name
                                            isNameAutofilled = true
                                        },
                                        label = { Text("${holding.symbol} (${NumberFormatHelper.formatShares(holding.totalShares, selectedMarket == Market.TAIWAN)})") },
                                        modifier = Modifier.testTag("chip_holding_${holding.symbol}")
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No active holdings available to sell",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("no_holdings_message")
                            )
                        }
                    }
                }

                // Stock code
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { newSymbol ->
                        symbol = newSymbol
                        val matchingAsset = savedAssets.find { it.symbol.equals(newSymbol.trim(), ignoreCase = true) }
                        val matchingH = currentHoldings.find { it.symbol.equals(newSymbol.trim(), ignoreCase = true) }
                        val resolvedName = matchingH?.name ?: matchingAsset?.name
                        if (resolvedName != null && (name.isBlank() || isNameAutofilled)) {
                            name = resolvedName
                            isNameAutofilled = true
                        }
                    },
                    label = { Text(if (selectedMarket == Market.TAIWAN) "Stock Code" else "Ticker Symbol") },
                    placeholder = { Text(if (selectedMarket == Market.TAIWAN) "e.g. 2330" else "e.g. AAPL") },
                    singleLine = true,
                    isError = transactionType == TransactionType.SELL && symbol.isNotBlank() && !ownsShares,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_symbol")
                )

                if (transactionType == TransactionType.SELL && symbol.isNotBlank() && !ownsShares) {
                    Text(
                        text = "No shares owned for this asset",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("error_no_shares_owned")
                    )
                }

                // Real-time max sellable shares hint
                if (transactionType == TransactionType.SELL && ownsShares) {
                    Text(
                        text = "Max sellable: ${NumberFormatHelper.formatShares(currentHoldingShares, selectedMarket == Market.TAIWAN)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("hint_max_sellable")
                    )
                }

                // Symbol autocomplete suggestions dropdown
                if (symbolSuggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("suggestions_symbol"),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            symbolSuggestions.forEach { asset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            symbol = asset.symbol
                                            name = asset.name
                                            isNameAutofilled = true
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("suggestion_symbol_${asset.symbol}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = asset.symbol,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = asset.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { newName ->
                        name = newName
                        isNameAutofilled = false
                    },
                    label = { Text("Name") },
                    placeholder = { Text(if (selectedMarket == Market.TAIWAN) "e.g. 台積電" else "e.g. Apple Inc.") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_name")
                )

                // Name autocomplete suggestions dropdown
                if (nameSuggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("suggestions_name"),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            nameSuggestions.forEach { asset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            name = asset.name
                                            if (symbol.isBlank()) {
                                                symbol = asset.symbol
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("suggestion_name_${asset.symbol}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = asset.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = asset.symbol,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Price
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(if (selectedMarket == Market.TAIWAN) "Unit Price (TWD)" else "Unit Price (USD) ($)") },
                    placeholder = { Text(if (selectedMarket == Market.TAIWAN) "e.g. 600" else "e.g. 120.20") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = priceText.isNotEmpty() && !isPriceValid,
                    supportingText = {
                        if (priceText.isNotEmpty() && !isPriceValid) {
                            Text("Price must be greater than 0")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_price")
                )

                // Shares
                OutlinedTextField(
                    value = sharesText,
                    onValueChange = { sharesText = it },
                    label = {
                        Text(
                            if (transactionType == TransactionType.SELL) {
                                if (selectedMarket == Market.TAIWAN) "Sold Shares (Integer)" else "Sold Shares"
                            } else {
                                if (selectedMarket == Market.TAIWAN) "Shares (Integer)" else "Shares"
                            }
                        )
                    },
                    placeholder = { Text(if (selectedMarket == Market.TAIWAN) "e.g. 1000" else "e.g. 2.5") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (selectedMarket == Market.TAIWAN) KeyboardType.Number else KeyboardType.Decimal
                    ),
                    isError = (sharesText.isNotEmpty() && !isSharesValid) || isOverselling,
                    supportingText = {
                        if (sharesText.isNotEmpty() && !isSharesValid && !isOverselling) {
                            Text(
                                if (selectedMarket == Market.TAIWAN) {
                                    "Shares must be a positive integer"
                                } else {
                                    "Shares must be greater than 0"
                                }
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_shares")
                )

                if (isOverselling) {
                    Text(
                        text = "Cannot sell more shares than currently held",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("error_oversell")
                    )
                }

                // Fee
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it },
                    label = { Text(if (selectedMarket == Market.TAIWAN) "Fee (TWD)" else "Fee (USD) ($)") },
                    placeholder = { Text(if (selectedMarket == Market.TAIWAN) "0" else "1.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = feeText.isNotEmpty() && !isFeeValid,
                    supportingText = {
                        if (feeText.isNotEmpty() && !isFeeValid) {
                            Text("Fee must be non-negative")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_fee")
                )

                // Tax (only for Sell transactions)
                if (transactionType == TransactionType.SELL) {
                    OutlinedTextField(
                        value = taxText,
                        onValueChange = {
                            taxText = it
                            isTaxManuallyEdited = true
                        },
                        label = { Text(if (selectedMarket == Market.TAIWAN) "Tax (TWD)" else "Tax (USD) ($)") },
                        placeholder = { Text(if (selectedMarket == Market.TAIWAN) "0" else "0.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = taxText.isNotEmpty() && !isTaxValid,
                        supportingText = {
                            if (taxText.isNotEmpty() && !isTaxValid) {
                                Text("Tax must be non-negative")
                            } else if (selectedMarket == Market.TAIWAN) {
                                Text("0.3% securities transaction tax")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_tax")
                    )
                }

                // Date
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Transaction Date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_date")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isFormValid) {
                        val transactedAt = parseDateToEpochMillis(dateText)
                        val transaction = Transaction(
                            market = selectedMarket,
                            symbol = symbol.trim().uppercase(),
                            name = name.trim().ifBlank { symbol.trim().uppercase() },
                            type = transactionType,
                            price = requireNotNull(parsedPrice),
                            shares = requireNotNull(parsedShares),
                            fee = parsedFee,
                            tax = if (transactionType == TransactionType.SELL) parsedTax else 0.0,
                            transactedAt = transactedAt
                        )
                        onSaveTransaction(transaction)
                    }
                },
                enabled = isFormValid,
                modifier = Modifier.testTag("button_save_transaction")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_cancel_transaction")
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun getTodayDateString(): String {
    return try {
        @Suppress("DEPRECATION")
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        @Suppress("DEPRECATION")
        val y = today.year.toString().padStart(4, '0')
        @Suppress("DEPRECATION")
        val m = today.monthNumber.toString().padStart(2, '0')
        @Suppress("DEPRECATION")
        val d = today.dayOfMonth.toString().padStart(2, '0')
        "$y-$m-$d"
    } catch (_: Exception) {
        "2026-10-31"
    }
}

private fun parseDateToEpochMillis(dateStr: String): Long {
    return try {
        val parts = dateStr.trim().split("-")
        if (parts.size == 3) {
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            val date = LocalDate(year, month, day)
            date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        } else {
            Clock.System.now().toEpochMilliseconds()
        }
    } catch (_: Exception) {
        Clock.System.now().toEpochMilliseconds()
    }
}
