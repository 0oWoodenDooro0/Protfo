package website.woodendoor.portfo.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import website.woodendoor.portfo.data.database.dao.AssetDao
import website.woodendoor.portfo.data.database.dao.TransactionDao
import website.woodendoor.portfo.data.database.entity.AssetEntity
import website.woodendoor.portfo.data.database.entity.TransactionEntity
import website.woodendoor.portfo.domain.model.Holding
import website.woodendoor.portfo.domain.model.Market
import website.woodendoor.portfo.domain.model.MarketSummary
import website.woodendoor.portfo.domain.model.Transaction
import website.woodendoor.portfo.domain.model.TransactionType
import website.woodendoor.portfo.domain.repository.PortfolioRepository
import website.woodendoor.portfo.domain.usecase.CalculatePortfolioUseCase

class PortfolioRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val assetDao: AssetDao,
    private val calculatePortfolioUseCase: CalculatePortfolioUseCase = CalculatePortfolioUseCase()
) : PortfolioRepository {

    override fun getTransactions(market: Market): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByMarket(market.code).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getHoldings(market: Market): Flow<List<Holding>> {
        return getTransactions(market).map { txs ->
            calculatePortfolioUseCase.execute(market, txs).holdings
        }
    }

    override fun getMarketSummary(market: Market): Flow<MarketSummary> {
        return getTransactions(market).map { txs ->
            calculatePortfolioUseCase.execute(market, txs).summary
        }
    }

    override suspend fun addTransaction(transaction: Transaction): Long {
        val entity = transaction.toEntity()
        val id = transactionDao.insertTransaction(entity)
        assetDao.upsertAsset(
            AssetEntity(
                symbol = transaction.symbol,
                market = transaction.market.code,
                name = transaction.name,
                currency = transaction.market.currency,
                updatedAt = transaction.transactedAt
            )
        )
        return id
    }

    override suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }
}

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    market = if (market == Market.US.code) Market.US else Market.TAIWAN,
    symbol = symbol,
    name = name,
    type = if (type == TransactionType.SELL.name) TransactionType.SELL else TransactionType.BUY,
    price = price,
    shares = shares,
    fee = fee,
    tax = tax,
    transactedAt = transactedAt,
    note = note
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    market = market.code,
    symbol = symbol,
    name = name,
    type = type.name,
    price = price,
    shares = shares,
    fee = fee,
    tax = tax,
    transactedAt = transactedAt,
    note = note
)
