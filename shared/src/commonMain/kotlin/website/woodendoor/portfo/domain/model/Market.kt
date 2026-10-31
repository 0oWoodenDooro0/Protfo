package website.woodendoor.portfo.domain.model

/**
 * Supported market classification according to Portfo domain model.
 * Taiwan: TWD currency, integer / odd lot shares.
 * US: USD currency, fractional shares.
 */
enum class Market(
    val code: String,
    val currency: String,
    val displayName: String
) {
    TAIWAN(code = "TW", currency = "TWD", displayName = "Taiwan"),
    US(code = "US", currency = "USD", displayName = "US")
}
