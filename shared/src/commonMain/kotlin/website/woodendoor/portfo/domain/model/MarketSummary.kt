package website.woodendoor.portfo.domain.model

data class MarketSummary(
    val market: Market,
    val totalCostBasis: Double = 0.0,
    val activeHoldingCount: Int = 0,
    val realizedPnL: Double = 0.0
)
