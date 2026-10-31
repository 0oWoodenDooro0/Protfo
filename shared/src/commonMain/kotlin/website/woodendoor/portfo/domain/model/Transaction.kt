package website.woodendoor.portfo.domain.model

data class Transaction(
    val id: Long = 0,
    val market: Market,
    val symbol: String,
    val name: String,
    val type: TransactionType,
    val price: Double,
    val shares: Double,
    val fee: Double = 0.0,
    val tax: Double = 0.0,
    val transactedAt: Long,
    val note: String? = null
)
