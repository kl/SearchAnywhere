package se.kalind.searchanywhere.data.apps

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppHistoryDao {
    @Query("SELECT * FROM app_item_history")
    fun getLatestHistory(): Flow<List<AppHistoryEntity>>

    @Upsert
    fun saveToHistory(item: AppHistoryEntity)

    @Delete
    fun deleteFromHistory(item: AppHistoryEntity)
}