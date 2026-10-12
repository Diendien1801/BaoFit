package com.visionfit.core.log

internal actual fun platformLog(level: LogLevel, area: String, message: String, error: Throwable?) {
    println("${level.name.first()}/${AppLog.TAG} [$area] $message")
    error?.let { println(it.stackTraceToString()) }
}
