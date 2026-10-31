package website.woodendoor.portfo.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import kotlinx.coroutines.flow.flowOf
import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.domain.model.Transaction
import website.woodendoor.portfo.domain.model.TransactionType
import website.woodendoor.portfo.domain.repository.PortfolioRepository
import website.woodendoor.portfo.ui.util.NumberFormatHelper

@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    repository: PortfolioRepository? = null,
    onDismiss: () -> Unit,
    onSaveTransaction: (Transaction) -> Unit
) {
    var priceText by remember(transaction) {
        mutableStateOf(
            if (transaction.price % 1.0 == 0.0) transaction.price.toLong().toString() else transaction.price.toString()
        )
    }
    var sharesText by remember(transaction) {
        mutableStateOf(
            if (transaction.market == Market.TAIWAN || transaction.shares % 1.0 == 0.0) {
                transaction.shares.toLong().toString()
            } else {
                transaction.shares.toString()
            }
        )
    }
    var feeText by remember(transaction) {
        mutableStateOf(
            if (transaction.fee % 1.0 == 0.0) transaction.fee.toLong().toString() else transaction.fee.toString()
        )
    }
    var taxText by remember(transaction) {
        mutableStateOf(
            if (transaction.tax % 1.0 == 0.0) transaction.tax.toLong().toString() else transaction.tax.toString()
        )
    }
    var dateText by remember(transaction) {
        mutableStateOf(NumberFormatHelper.formatDate(transaction.transactedAt))
    }
    var isTaxManuallyEdited by remember { mutableStateOf(false) }

    // Collect holdings to check sell validation guards
    val holdingsFlow = remember(transaction.market, repository) {
        repository?.getHoldings(transaction.market) ?: flowOf(emptyList())
    }
    val currentHoldings by holdingsFlow.collectAsState(initial = emptyList())

    val matchingHolding = currentHoldings.find { it.symbol.equals(transaction.symbol, ignoreCase = true) }
    val currentHoldingShares = matchingHolding?.totalShares ?: 0.0

    // For a sell transaction, max sellable shares is currently remaining shares + shares in this transaction
    val maxSellableShares = if (transaction.type == TransactionType.SELL) {
        currentHoldingShares + transaction.shares
    } else {
        Double.MAX_VALUE
    }

    val parsedPrice = priceText.toDoubleOrNull()
    val isPriceValid = parsedPrice != null && parsedPrice > 0.0

    val parsedShares = if (transaction.market == Market.TAIWAN) {
        sharesText.toLongOrNull()?.toDouble()
    } else {
        sharesText.toDoubleOrNull()
    }

    val isOverselling = transaction.type == TransactionType.SELL && parsedShares != null && (parsedShares > maxSellableShares + 1e-6)

    val isSharesValid = if (transaction.type == TransactionType.SELL) {
        parsedShares != null && parsedShares > 0.0 && !isOverselling
    } else {
        parsedShares != null && parsedShares > 0.0
    }

    val parsedFee = feeText.toDoubleOrNull() ?: 0.0
    val isFeeValid = feeText.isBlank() || (parsedFee >= 0.0)

    val parsedTax = taxText.toDoubleOrNull() ?: 0.0
    val isTaxValid = taxText.isBlank() || (parsedTax >= 0.0)

    val isFormValid = isPriceValid && isSharesValid && isFeeValid && (transaction.type == TransactionType.BUY || isTaxValid)

    // Recalculate Taiwan tax on price or shares change if not manually edited
    LaunchedEffect(parsedPrice, parsedShares, isTaxManuallyEdited) {
        if (transaction.type == TransactionType.SELL && transaction.market == Market.TAIWAN && !isTaxManuallyEdited) {
            if (parsedPrice != null && parsedShares != null && parsedPrice > 0.0 && parsedShares > 0.0) {
                val estimatedTax = floor(parsedPrice * parsedShares * 0.003).toLong()
                taxText = estimatedTax.toString()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_edit_transaction"),
        title = {
            Text(
                text = "編輯交易",
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
                // Readonly Asset header
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = transaction.symbol,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (transaction.name.isNotBlank() && transaction.name != transaction.symbol) {
                                Text(
                                    text = transaction.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (transaction.type == TransactionType.BUY) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        ) {
                            Text(
                                text = if (transaction.type == TransactionType.BUY) "買進" else "賣出",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (transaction.type == TransactionType.BUY) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (transaction.type == TransactionType.SELL) {
                    Text(
                        text = "可賣數量: ${NumberFormatHelper.formatShares(maxSellableShares, transaction.market == Market.TAIWAN)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("hint_max_sellable")
                    )
                }

                // Price
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("成交單價") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = priceText.isNotEmpty() && !isPriceValid,
                    supportingText = {
                        if (priceText.isNotEmpty() && !isPriceValid) {
                            Text("單價必須大於 0")
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
                        Text(if (transaction.type == TransactionType.SELL) "賣出股數" else "買進股數")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (transaction.market == Market.TAIWAN) KeyboardType.Number else KeyboardType.Decimal
                    ),
                    isError = (sharesText.isNotEmpty() && !isSharesValid) || isOverselling,
                    supportingText = {
                        if (sharesText.isNotEmpty() && !isSharesValid && !isOverselling) {
                            Text(
                                if (transaction.market == Market.TAIWAN) {
                                    "股數必須為正整數"
                                } else {
                                    "股數必須大於 0"
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
                        text = "賣出股數不可大於現有持股",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("error_oversell")
                    )
                }

                // Fee
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it },
                    label = { Text("手續費") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = feeText.isNotEmpty() && !isFeeValid,
                    supportingText = {
                        if (feeText.isNotEmpty() && !isFeeValid) {
                            Text("手續費不可為負數")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_fee")
                )

                // Tax (only for Sell transactions)
                if (transaction.type == TransactionType.SELL) {
                    OutlinedTextField(
                        value = taxText,
                        onValueChange = {
                            taxText = it
                            isTaxManuallyEdited = true
                        },
                        label = { Text("證券交易稅") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = taxText.isNotEmpty() && !isTaxValid,
                        supportingText = {
                            if (taxText.isNotEmpty() && !isTaxValid) {
                                Text("證交稅不可為負數")
                            } else if (transaction.market == Market.TAIWAN) {
                                Text("0.3% 證券交易稅")
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
                    label = { Text("交易日期") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_date")
                )
            }
        },
        confirmButton = {
            IconButton(
                onClick = {
                    if (isFormValid) {
                        val transactedAt = NumberFormatHelper.parseDateToEpochMillis(dateText)
                        val updated = transaction.copy(
                            price = requireNotNull(parsedPrice),
                            shares = requireNotNull(parsedShares),
                            fee = parsedFee,
                            tax = if (transaction.type == TransactionType.SELL) parsedTax else 0.0,
                            transactedAt = transactedAt
                        )
                        onSaveTransaction(updated)
                    }
                },
                enabled = isFormValid,
                modifier = Modifier.testTag("button_save_transaction")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "儲存"
                )
            }
        },
        dismissButton = {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_cancel_transaction")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消"
                )
            }
        }
    )
}
