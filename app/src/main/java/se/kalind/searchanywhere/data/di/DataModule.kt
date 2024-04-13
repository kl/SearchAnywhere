package se.kalind.searchanywhere.data.di

import android.content.Context
import android.os.Environment
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import se.kalind.searchanywhere.data.SearchAnywhereDatabase
import se.kalind.searchanywhere.data.apps.AppHistoryDao
import se.kalind.searchanywhere.data.apps.DefaultAppsRepository
import se.kalind.searchanywhere.data.files.AnlocateLibrary
import se.kalind.searchanywhere.data.files.DefaultFilesRepository
import se.kalind.searchanywhere.data.files.FileHistoryDao
import se.kalind.searchanywhere.data.settings.DefaultSettingsRepository
import se.kalind.searchanywhere.data.settings.SettingHistoryDao
import se.kalind.searchanywhere.domain.repo.AppsRepository
import se.kalind.searchanywhere.domain.repo.FilesRepository
import se.kalind.searchanywhere.domain.repo.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideSettingsRepository(
        settingHistoryDao: SettingHistoryDao,
    ): SettingsRepository {
        return DefaultSettingsRepository(settingHistoryDao)
    }

    @Singleton
    @Provides
    fun provideAppsRepository(
        @ApplicationContext context: Context,
        appHistoryDao: AppHistoryDao,
    ): AppsRepository {
        return DefaultAppsRepository(context, appHistoryDao)
    }

    @Singleton
    @Provides
    fun provideFilesRepository(
        @ApplicationContext context: Context,
        anlocateLibrary: AnlocateLibrary,
        fileHistoryDao: FileHistoryDao,
    ): FilesRepository {
        return DefaultFilesRepository(
            lib = anlocateLibrary,
            fileHistoryDao = fileHistoryDao,
            databaseFilePath = context.filesDir.absolutePath + "/anlocate.bin",
            tempDirPath = context.filesDir.absolutePath + "/anlocate_temp",
            scanDirRootPath = Environment.getExternalStorageDirectory().absolutePath,
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideApplicationDatabase(@ApplicationContext context: Context): SearchAnywhereDatabase {
        return Room.databaseBuilder(
            context,
            SearchAnywhereDatabase::class.java,
            "search_anywhere_database"
        ).build()
    }

    @Provides
    fun provideAppHistoryDao(database: SearchAnywhereDatabase): AppHistoryDao {
        return database.appHistoryDao()
    }

    @Provides
    fun provideSettingHistoryDao(database: SearchAnywhereDatabase): SettingHistoryDao {
        return database.settingHistoryDao()
    }

    @Provides
    fun provideFileHistoryDao(database: SearchAnywhereDatabase): FileHistoryDao {
        return database.fileHistoryDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NativeCodeModule {
    @Provides
    fun provideAnlocateLibrary(): AnlocateLibrary {
        return AnlocateLibrary()
    }
}