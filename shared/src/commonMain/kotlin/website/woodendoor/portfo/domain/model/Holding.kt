package website.woodendoor.portfo.domain.model

data class Holding(
    val symbol: String,
    val name: String,
    val market: Market,
    val currency: String,
    val totalShares: Double,
    val avgCostPerShare: Double,
    val totalCostBasis: Double,
    val realizedPnL: Double = 0.0
)
