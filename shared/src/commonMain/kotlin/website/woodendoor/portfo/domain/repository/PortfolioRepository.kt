package website.woodendoor.portfo.domain.repository

import kotlinx.coroutines.flow.Flow
import website.woodendoor.portfo.domain.model.Holding
import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.domain.model.MarketSummary
import website.woodendoor.portfo.domain.model.Transaction

interface PortfolioRepository {
    fun getTransactions(market: Market): Flow<List<Transaction>>
    fun getHoldings(market: Market): Flow<List<Holding>>
    fun getMarketSummary(market: Market): Flow<MarketSummary>
    suspend fun addTransaction(transaction: Transaction): Long
    suspend fun deleteTransaction(id: Long)
    suspend fun getTransactionById(id: Long): Transaction?
}
