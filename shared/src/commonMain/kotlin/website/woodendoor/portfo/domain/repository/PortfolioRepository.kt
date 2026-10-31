package website.woodendoor.portfo.domain.repository

import kotlinx.coroutines.flow.Flow
import website.woodendoor.portfo.domain.model.Asset
import website.woodendoor.portfo.domain.model.Holding
import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.domain.model.MarketSummary
import website.woodendoor.portfo.domain.model.Transaction

interface PortfolioRepository {
    fun getTransactions(market: Market): Flow<List<Transaction>>
    fun getHoldings(market: Market): Flow<List<Holding>>
    fun getMarketSummary(market: Market): Flow<MarketSummary>
    fun getAssets(market: Market): Flow<List<Asset>>
    suspend fun searchAssets(market: Market, query: String): List<Asset>
    suspend fun getAsset(market: Market, symbol: String): Asset?
    suspend fun addTransaction(transaction: Transaction): Long
    suspend fun deleteTransaction(id: Long)
    suspend fun getTransactionById(id: Long): Transaction?
}
