package se.kalind.searchanywhere.data.files

import android.annotation.SuppressLint
import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@SuppressLint("KotlinNullnessAnnotation")
@Entity(tableName = "file_item_history")
data class FileHistoryEntity(
    @PrimaryKey @ColumnInfo(name = "full_path") val fullPath: String,
    @NonNull @ColumnInfo(name = "update_time") val updateTime: Long,
)