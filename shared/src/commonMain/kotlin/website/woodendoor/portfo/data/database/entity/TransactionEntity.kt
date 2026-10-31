package website.woodendoor.portfo.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val market: String,
    val symbol: String,
    val name: String,
    val type: String,
    val price: Double,
    val shares: Double,
    val fee: Double = 0.0,
    val tax: Double = 0.0,
    val transactedAt: Long,
    val note: String? = null
)
