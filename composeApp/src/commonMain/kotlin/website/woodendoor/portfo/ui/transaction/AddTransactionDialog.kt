package website.woodendoor.portfo.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.domain.model.Transaction
import website.woodendoor.portfo.domain.model.TransactionType

@Composable
fun AddTransactionDialog(
    initialMarket: Market = Market.TAIWAN,
    onDismiss: () -> Unit,
    onSaveTransaction: (Transaction) -> Unit
) {
    var selectedMarket by remember { mutableStateOf(initialMarket) }
    var symbol by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var sharesText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("0") }
    var dateText by remember { mutableStateOf(getTodayDateString()) }

    val parsedPrice = priceText.toDoubleOrNull()
    val isPriceValid = parsedPrice != null && parsedPrice > 0.0

    val parsedShares = if (selectedMarket == Market.TAIWAN) {
        sharesText.toLongOrNull()?.toDouble()
    } else {
        sharesText.toDoubleOrNull()
    }
    val isSharesValid = parsedShares != null && parsedShares > 0.0

    val parsedFee = feeText.toDoubleOrNull() ?: 0.0
    val isFeeValid = feeText.isBlank() || (parsedFee >= 0.0)

    val isFormValid = symbol.isNotBlank() && isPriceValid && isSharesValid && isFeeValid

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_add_transaction"),
        title = {
            Text(
                text = "Record Buy Transaction",
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
                        onClick = { selectedMarket = Market.TAIWAN },
                        label = { Text("Taiwan (TWD)") },
                        modifier = Modifier.testTag("input_market_tw")
                    )
                    FilterChip(
                        selected = selectedMarket == Market.US,
                        onClick = { selectedMarket = Market.US },
                        label = { Text("US (USD)") },
                        modifier = Modifier.testTag("input_market_us")
                    )
                }

                // Stock code
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Stock Code") },
                    placeholder = { Text(if (selectedMarket == Market.TAIWAN) "e.g. 2330" else "e.g. AAPL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_symbol")
                )

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text(if (selectedMarket == Market.TAIWAN) "e.g. 台積電" else "e.g. Apple Inc.") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_name")
                )

                // Price
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(if (selectedMarket == Market.TAIWAN) "Unit Price (TWD)" else "Unit Price (USD)") },
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
                    label = { Text(if (selectedMarket == Market.TAIWAN) "Shares (Integer)" else "Shares") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (selectedMarket == Market.TAIWAN) KeyboardType.Number else KeyboardType.Decimal
                    ),
                    isError = sharesText.isNotEmpty() && !isSharesValid,
                    supportingText = {
                        if (sharesText.isNotEmpty() && !isSharesValid) {
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

                // Fee
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it },
                    label = { Text(if (selectedMarket == Market.TAIWAN) "Fee (TWD)" else "Fee (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = feeText.isNotEmpty() && !isFeeValid,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_fee")
                )

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
                            type = TransactionType.BUY,
                            price = parsedPrice,
                            shares = parsedShares,
                            fee = parsedFee,
                            tax = 0.0,
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
