package se.kalind.searchanywhere

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import se.kalind.searchanywhere.presentation.MainActivityReference
import javax.inject.Inject

@HiltAndroidApp
class SearchAnywhereApplication : Application() {

    @Inject
    lateinit var mainActivityRef: MainActivityReference

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(mainActivityRef)
    }
}
