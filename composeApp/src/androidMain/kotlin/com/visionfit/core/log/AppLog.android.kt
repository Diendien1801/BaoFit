package com.visionfit.core.log

import android.util.Log

internal actual fun platformLog(level: LogLevel, area: String, message: String, error: Throwable?) {
    val line = "[$area] $message"
    when (level) {
        LogLevel.DEBUG -> Log.d(AppLog.TAG, line, error)
        LogLevel.WARN -> Log.w(AppLog.TAG, line, error)
        LogLevel.ERROR -> Log.e(AppLog.TAG, line, error)
    }
}
