package se.kalind.searchanywhere.data.apps

import android.annotation.SuppressLint
import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@SuppressLint("KotlinNullnessAnnotation")
@Entity(tableName = "app_item_history")
data class AppHistoryEntity(
    @PrimaryKey val id: String,
    @NonNull val label: String,
    @NonNull @ColumnInfo(name = "package_name") val packageName: String,
    @NonNull @ColumnInfo(name = "activity_name") val activityName: String,
    @NonNull @ColumnInfo(name = "update_time") val updateTime: Long,
)