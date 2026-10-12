package com.visionfit.core.log

/**
 * Debug log shared by every platform. Android writes to Logcat under the tag [TAG] (filter with
 * `tag:VisionFit`), iOS to the Xcode console, desktop to standard output.
 *
 * [area] says which part of the app wrote the line (`Auth`, `Profile`, …). Never pass
 * passwords or tokens.
 */
object AppLog {
    const val TAG = "VisionFit"

    fun d(area: String, message: String) = platformLog(LogLevel.DEBUG, area, message, null)

    fun w(area: String, message: String, error: Throwable? = null) = platformLog(LogLevel.WARN, area, message, error)

    fun e(area: String, message: String, error: Throwable? = null) = platformLog(LogLevel.ERROR, area, message, error)
}

internal enum class LogLevel { DEBUG, WARN, ERROR }

internal expect fun platformLog(level: LogLevel, area: String, message: String, error: Throwable?)
