package com.chriscartland.batterybutler.presentationcore.util

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * Android implementation of [AppRestarter]: kills the current process and
 * launches a fresh instance of the app's launcher Intent. This guarantees
 * a fully fresh DI graph (including a fresh `DynamicDatabaseProvider` and
 * `AppDatabase`) which avoids the in-process Flow re-bind issue documented
 * in bd issue bb-lg42.
 */
class AndroidAppRestarter(
    context: Context,
) : AppRestarter {
    private val appContext = context.applicationContext
    private val activityRef: Activity? = context as? Activity

    override fun restart() {
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            appContext.startActivity(launchIntent)
        }
        // finishAffinity removes the current task; killProcess terminates the
        // process so the new Intent starts a fresh process with fresh DI.
        activityRef?.finishAffinity()
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
