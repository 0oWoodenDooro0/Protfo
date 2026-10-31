package website.woodendoor.portfo.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import website.woodendoor.portfo.data.database.dao.AssetDao
import website.woodendoor.portfo.data.database.dao.TransactionDao
import website.woodendoor.portfo.data.database.entity.AssetEntity
import website.woodendoor.portfo.data.database.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, AssetEntity::class],
    version = 1,
    exportSchema = false
)
@ConstructedBy(PortfoDatabaseConstructor::class)
abstract class PortfoDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun assetDao(): AssetDao
}

// Room compiler generates the actual implementation
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object PortfoDatabaseConstructor : RoomDatabaseConstructor<PortfoDatabase> {
    override fun initialize(): PortfoDatabase
}
