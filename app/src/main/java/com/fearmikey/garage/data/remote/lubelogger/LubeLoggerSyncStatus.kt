package com.fearmikey.garage.data.remote.lubelogger

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * The real outcome of talking to the LubeLogger server, as last observed by
 * [com.fearmikey.garage.notification.lubelogger.LubeLoggerSyncWorker]. Settings uses this instead
 * of "credentials are saved" to show whether Garage is actually connected.
 */
sealed interface LubeLoggerSyncStatus {
    /** Configured, but no sync has finished yet (or the connection settings just changed). */
    data object NeverSynced : LubeLoggerSyncStatus

    data class Syncing(val lastSuccessAt: Long?) : LubeLoggerSyncStatus

    data class Success(val at: Long) : LubeLoggerSyncStatus

    data class Failed(val at: Long, val message: String, val lastSuccessAt: Long?) : LubeLoggerSyncStatus
}

/** A short, user-facing reason for a failed sync. */
fun describeLubeLoggerFailure(error: Throwable): String = when (error) {
    is UnknownHostException -> "Server not found. Check the URL and your internet connection."
    is ConnectException -> "Can't reach the server."
    is SocketTimeoutException -> "The server took too long to respond."
    is SSLException -> "Secure connection failed (SSL/certificate problem)."
    is IOException -> "Network error: ${error.message ?: error.javaClass.simpleName}"
    else -> error.message ?: error.javaClass.simpleName
}

/** A short, user-facing reason for an HTTP error response. */
fun describeLubeLoggerHttpFailure(code: Int): String = when (code) {
    401, 403 -> "LubeLogger rejected the username/password or API key (HTTP $code)."
    404 -> "No LubeLogger API at this URL (HTTP 404). Check the URL."
    in 500..599 -> "LubeLogger server error (HTTP $code)."
    else -> "Unexpected response from LubeLogger (HTTP $code)."
}
