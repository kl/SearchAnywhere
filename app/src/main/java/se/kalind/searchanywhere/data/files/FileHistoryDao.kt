package se.kalind.searchanywhere.data.files

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FileHistoryDao {
    @Query("SELECT * FROM file_item_history")
    fun getLatestHistory(): Flow<List<FileHistoryEntity>>

    @Upsert
    fun saveToHistory(item: FileHistoryEntity)
}