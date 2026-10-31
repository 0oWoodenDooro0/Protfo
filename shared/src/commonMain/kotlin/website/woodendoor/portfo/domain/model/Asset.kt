package website.woodendoor.portfo.domain.model

data class Asset(
    val symbol: String,
    val market: Market,
    val name: String,
    val currency: String
)
