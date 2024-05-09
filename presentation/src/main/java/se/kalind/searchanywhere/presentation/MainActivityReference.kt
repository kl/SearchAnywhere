package se.kalind.searchanywhere.presentation

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import se.kalind.searchanywhere.presentation.MainActivity

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