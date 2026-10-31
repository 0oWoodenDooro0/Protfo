package website.woodendoor.portfo.ui.market

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import website.woodendoor.portfo.domain.model.Holding
import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.domain.model.MarketSummary
import website.woodendoor.portfo.ui.util.NumberFormatHelper

@Composable
fun UsMarketContainer(
    modifier: Modifier = Modifier,
    summary: MarketSummary = MarketSummary(market = Market.US),
    holdings: List<Holding> = emptyList(),
    onAddTransactionClick: () -> Unit = {},
    onHoldingClick: (Holding) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("us_market_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // US Cost Basis Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("us_summary_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "總投資成本",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = NumberFormatHelper.formatCurrency(summary.totalCostBasis, prefix = "$ ", decimals = 2),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("us_total_cost_basis")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                    val pnlColor = when {
                        summary.realizedPnL > 1e-6 -> if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                        summary.realizedPnL < -1e-6 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val formattedPnL = if (summary.realizedPnL > 1e-6) {
                        "+${NumberFormatHelper.formatCurrency(summary.realizedPnL, prefix = "$ ", decimals = 2)}"
                    } else {
                        NumberFormatHelper.formatCurrency(summary.realizedPnL, prefix = "$ ", decimals = 2)
                    }
                    Text(
                        text = "已實現損益: $formattedPnL",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (summary.realizedPnL != 0.0) FontWeight.SemiBold else FontWeight.Normal,
                        color = pnlColor,
                        modifier = Modifier.testTag("us_realized_pnl")
                    )
                    Text(
                        text = "持股檔數: ${summary.activeHoldingCount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("us_active_holding_count")
                    )
                }
            }
        }

        // Holdings section
        if (holdings.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("us_empty_holdings"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "尚未建立美股持股紀錄",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "點擊下方 + 記錄交易",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("us_holdings_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = holdings, key = { it.symbol }) { holding ->
                    UsHoldingCardVariantB(
                        holding = holding,
                        onClick = { onHoldingClick(holding) }
                    )
                }
            }
        }
    }
}

/**
 * US Holding card rendered in clean single-box layout:
 * Displays symbol and name on the left, total cost and quantity on the right without clutter.
 */
@Composable
fun UsHoldingCardVariantB(
    holding: Holding,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("holding_card_${holding.symbol}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Symbol & Name
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = holding.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (holding.name.isNotBlank() && holding.name != holding.symbol) {
                    Text(
                        text = holding.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Total Cost & Quantity
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "$ ${NumberFormatHelper.formatDecimal(holding.totalCostBasis, 2)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("holding_cost_basis_${holding.symbol}")
                )
                Text(
                    text = NumberFormatHelper.formatShares(holding.totalShares, isIntegerMarket = false),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("holding_shares_${holding.symbol}")
                )
            }
        }
    }
}
