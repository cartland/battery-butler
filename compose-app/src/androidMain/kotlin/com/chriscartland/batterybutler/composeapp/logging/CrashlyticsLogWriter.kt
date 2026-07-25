package com.chriscartland.batterybutler.composeapp.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Forwards `Warn`/`Error`/`Assert` Kermit log calls to Crashlytics, so every existing
 * `Logger.e(tag, throwable) { ... }` / `Logger.w(tag, throwable) { ... }` call site across the
 * shared KMP modules -- auth token refresh, sync, device-image upload, etc. -- gets non-fatal
 * crash reporting for free, without touching each call site individually. Installed once via
 * [co.touchlab.kermit.Logger.addLogWriter] in `BatteryButlerApplication.onCreate()`; it doesn't
 * replace the existing (Logcat) writers, only adds to them.
 *
 * Safe with an unconfigured/mock `google-services.json`: [FirebaseCrashlytics] queues reports
 * locally and only attempts a network upload later, so a fake project id just means the upload
 * silently fails rather than anything crashing at log time.
 */
class CrashlyticsLogWriter : LogWriter() {
    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) {
        if (severity < Severity.Warn) return
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.log("[$tag] $message")
        if (throwable != null) {
            crashlytics.recordException(throwable)
        }
    }
}
