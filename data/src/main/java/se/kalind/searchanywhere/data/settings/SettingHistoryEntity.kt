package se.kalind.searchanywhere.data.settings

import android.annotation.SuppressLint
import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@SuppressLint("KotlinNullnessAnnotation")
@Entity(tableName = "setting_item_history")
data class SettingHistoryEntity(
    @PrimaryKey val id: String,
    @NonNull val fieldName: String,
    @NonNull val fieldValue: String,
    @NonNull @ColumnInfo(name = "update_time") val updateTime: Long,
)