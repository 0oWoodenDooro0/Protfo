package website.woodendoor.portfo.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import website.woodendoor.portfo.data.database.entity.TransactionEntity

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT * FROM transactions WHERE market = :market ORDER BY transactedAt ASC, id ASC")
    fun getTransactionsByMarket(market: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE symbol = :symbol AND market = :market ORDER BY transactedAt ASC, id ASC")
    fun getTransactionsBySymbol(symbol: String, market: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY transactedAt ASC, id ASC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?
}
