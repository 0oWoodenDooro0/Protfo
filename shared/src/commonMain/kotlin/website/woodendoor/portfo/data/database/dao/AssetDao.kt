package website.woodendoor.portfo.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import website.woodendoor.portfo.data.database.entity.AssetEntity

@Dao
interface AssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAsset(asset: AssetEntity)

    @Query("SELECT * FROM assets WHERE market = :market ORDER BY updatedAt DESC")
    fun getAssetsByMarket(market: String): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE symbol = :symbol AND market = :market")
    suspend fun getAsset(symbol: String, market: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE market = :market AND (symbol LIKE :query || '%' OR name LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    suspend fun searchAssets(market: String, query: String): List<AssetEntity>
}
