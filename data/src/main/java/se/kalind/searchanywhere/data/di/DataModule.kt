package se.kalind.searchanywhere.data.di

import android.content.Context
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import se.kalind.searchanywhere.data.SearchAnywhereDatabase
import se.kalind.searchanywhere.data.apps.AppHistoryDao
import se.kalind.searchanywhere.data.apps.DefaultAppsRepository
import se.kalind.searchanywhere.data.files.AnlocateLibrary
import se.kalind.searchanywhere.data.files.DefaultFilesRepository
import se.kalind.searchanywhere.data.files.FileHistoryDao
import se.kalind.searchanywhere.data.prefs.DefaultPreferencesRepository
import se.kalind.searchanywhere.data.settings.DefaultSettingsRepository
import se.kalind.searchanywhere.data.settings.SettingHistoryDao
import se.kalind.searchanywhere.domain.repo.AppsRepository
import se.kalind.searchanywhere.domain.repo.FilesRepository
import se.kalind.searchanywhere.domain.repo.PreferencesRepository
import se.kalind.searchanywhere.domain.repo.SettingsRepository
import javax.inject.Named
import javax.inject.Singleton

// this api is stupid
private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideSettingsRepository(
        settingHistoryDao: SettingHistoryDao,
        @Named("io") ioDispatcher: CoroutineDispatcher,
        appScope: CoroutineScope,
    ): SettingsRepository {
        return DefaultSettingsRepository(settingHistoryDao, ioDispatcher, appScope)
    }

    @Singleton
    @Provides
    fun provideAppsRepository(
        @ApplicationContext context: Context,
        appHistoryDao: AppHistoryDao,
        @Named("io") ioDispatcher: CoroutineDispatcher,
        appScope: CoroutineScope,
    ): AppsRepository {
        return DefaultAppsRepository(context, appHistoryDao, ioDispatcher, appScope)
    }

    @Singleton
    @Provides
    fun provideFilesRepository(
        @ApplicationContext context: Context,
        anlocateLibrary: AnlocateLibrary,
        fileHistoryDao: FileHistoryDao,
        @Named("io") ioDispatcher: CoroutineDispatcher,
        appScope: CoroutineScope,
    ): FilesRepository {
        return DefaultFilesRepository(
            lib = anlocateLibrary,
            fileHistoryDao = fileHistoryDao,
            databaseFilePath = context.filesDir.absolutePath + "/anlocate.bin",
            tempDirPath = context.filesDir.absolutePath + "/anlocate_temp",
            scanDirRootPath = Environment.getExternalStorageDirectory().absolutePath,
            ioDispatcher = ioDispatcher,
            appScope = appScope
        )
    }

    @Singleton
    @Provides
    fun providePreferencesRepository(
        dataStore: DataStore<Preferences>,
        @Named("io") ioDispatcher: CoroutineDispatcher,
        appScope: CoroutineScope,
    ): PreferencesRepository {
        return DefaultPreferencesRepository(
            dataStore = dataStore,
            appScope = appScope,
            ioDispatcher = ioDispatcher
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

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return context.userDataStore
    }
}
