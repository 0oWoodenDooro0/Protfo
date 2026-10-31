package website.woodendoor.portfo.domain.usecase

import website.woodendoor.portfo.domain.model.Holding
import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.domain.model.MarketSummary
import website.woodendoor.portfo.domain.model.Transaction
import website.woodendoor.portfo.domain.model.TransactionType

data class PortfolioCalculationResult(
    val holdings: List<Holding>,
    val summary: MarketSummary
)

class CalculatePortfolioUseCase {

    fun execute(market: Market, transactions: List<Transaction>): PortfolioCalculationResult {
        val marketTransactions = transactions
            .filter { it.market == market }
            .sortedWith(compareBy<Transaction> { it.transactedAt }.thenBy { it.id })

        val grouped = marketTransactions.groupBy { it.symbol }

        val activeHoldings = mutableListOf<Holding>()
        var marketRealizedPnL = 0.0

        for ((symbol, symbolTxs) in grouped) {
            var currentShares = 0.0
            var currentCostBasis = 0.0
            var symbolRealizedPnL = 0.0
            var assetName = symbol

            for (tx in symbolTxs) {
                if (tx.name.isNotBlank()) {
                    assetName = tx.name
                }
                when (tx.type) {
                    TransactionType.BUY -> {
                        val buyCost = (tx.price * tx.shares) + tx.fee
                        currentCostBasis += buyCost
                        currentShares += tx.shares
                    }
                    TransactionType.SELL -> {
                        val avgCost = if (currentShares > 0.0) currentCostBasis / currentShares else 0.0
                        val costOfSold = avgCost * tx.shares
                        val netProceeds = (tx.price * tx.shares) - tx.fee - tx.tax
                        val pnl = netProceeds - costOfSold
                        symbolRealizedPnL += pnl
                        currentCostBasis -= costOfSold
                        currentShares -= tx.shares

                        if (currentShares <= 1e-6) {
                            currentShares = 0.0
                            currentCostBasis = 0.0
                        }
                    }
                }
            }

            marketRealizedPnL += symbolRealizedPnL

            if (currentShares > 1e-6) {
                val avgCost = currentCostBasis / currentShares
                activeHoldings.add(
                    Holding(
                        symbol = symbol,
                        name = assetName,
                        market = market,
                        currency = market.currency,
                        totalShares = currentShares,
                        avgCostPerShare = avgCost,
                        totalCostBasis = currentCostBasis,
                        realizedPnL = symbolRealizedPnL
                    )
                )
            }
        }

        val totalCostBasis = activeHoldings.sumOf { it.totalCostBasis }
        val summary = MarketSummary(
            market = market,
            totalCostBasis = totalCostBasis,
            activeHoldingCount = activeHoldings.size,
            realizedPnL = marketRealizedPnL
        )

        return PortfolioCalculationResult(
            holdings = activeHoldings,
            summary = summary
        )
    }

    operator fun invoke(market: Market, transactions: List<Transaction>): PortfolioCalculationResult =
        execute(market, transactions)
}
