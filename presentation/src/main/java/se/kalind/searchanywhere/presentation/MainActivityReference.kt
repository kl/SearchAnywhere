package se.kalind.searchanywhere.presentation

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log

/**
 * Some APIs require an Activity context and not just the application context, for example
 * Context#startActivity. This class exists as a reference to the MainActivity class, and can be
 * injected into classes that need to access the MainActivity.
 */
class MainActivityReference: Application.ActivityLifecycleCallbacks {

    var mainActivity: MainActivity? = null

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.d("SearchAnywhere", "onActivityCreated")
        if (activity is MainActivity) {
            mainActivity = activity
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        Log.d("SearchAnywhere", "onActivityDestroyed")
        if (activity is MainActivity) {
            mainActivity = null
        }
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}