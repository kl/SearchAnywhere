package se.kalind.searchanywhere.presentation.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import se.kalind.searchanywhere.domain.usecases.HistoryUseCase
import se.kalind.searchanywhere.domain.usecases.ItemOpener
import se.kalind.searchanywhere.presentation.DefaultItemOpener
import se.kalind.searchanywhere.presentation.MainActivityReference
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PresentationModule {

    @Singleton
    @Provides
    fun provideItemOpener(
        mainActivityRef: MainActivityReference,
        historyUseCase: HistoryUseCase,
    ): ItemOpener {
        return DefaultItemOpener(mainActivityRef, historyUseCase)
    }

    @Singleton
    @Provides
    fun provideMainActivityReference(): MainActivityReference {
        return MainActivityReference()
    }
}
