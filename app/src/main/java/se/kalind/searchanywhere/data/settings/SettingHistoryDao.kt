package se.kalind.searchanywhere.data.settings

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingHistoryDao {
    @Query("SELECT * FROM setting_item_history")
    fun getLatestHistory(): Flow<List<SettingHistoryEntity>>

    @Upsert
    fun saveToHistory(item: SettingHistoryEntity)
}