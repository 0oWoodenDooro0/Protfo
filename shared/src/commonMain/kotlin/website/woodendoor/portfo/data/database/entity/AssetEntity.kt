package website.woodendoor.portfo.data.database.entity

import androidx.room.Entity

@Entity(tableName = "assets", primaryKeys = ["symbol", "market"])
data class AssetEntity(
    val symbol: String,
    val market: String,
    val name: String,
    val currency: String,
    val updatedAt: Long
)
