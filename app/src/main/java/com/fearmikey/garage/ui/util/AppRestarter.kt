package com.fearmikey.garage.ui.util

import android.content.Context
import android.content.Intent
import com.fearmikey.garage.MainActivity

/**
 * After a database restore, the live [com.fearmikey.garage.data.local.GarageDatabase]
 * connection has been closed and its backing files replaced on disk. Rather
 * than attempt to hot-swap every Hilt-provided repository/DAO/ViewModel that
 * might reference the old connection, we simply relaunch the app's main
 * activity in a fresh task and kill this process -- the next launch opens
 * Room against the restored files with a completely clean slate.
 */
object AppRestarter {
    fun restart(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
