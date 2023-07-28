package se.kalind.searchanywhere.data

import androidx.room.Database
import androidx.room.RoomDatabase
import se.kalind.searchanywhere.data.apps.AppHistoryDao
import se.kalind.searchanywhere.data.apps.AppHistoryEntity
import se.kalind.searchanywhere.data.settings.SettingHistoryDao
import se.kalind.searchanywhere.data.settings.SettingHistoryEntity

@Database(entities = [AppHistoryEntity::class, SettingHistoryEntity::class], version = 1)
abstract class SearchAnywhereDatabase : RoomDatabase() {
    abstract fun appHistoryDao(): AppHistoryDao
    abstract fun settingHistoryDao(): SettingHistoryDao
}