package com.fearmikey.garage.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.fearmikey.garage.MainActivity
import com.fearmikey.garage.R
import com.fearmikey.garage.data.repository.ReminderStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts/updates a single notification per reminder, keyed by the reminder's
 * own database id so [ReminderCheckWorker] can safely re-run daily and just
 * refresh the same notification in place instead of stacking duplicates.
 */
@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_notification_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun notifyDue(
        notificationId: Int,
        taskName: String,
        vehicleLabel: String,
        status: ReminderStatus,
        vehicleId: Long = MainActivity.NO_VEHICLE_ID_EXTRA,
    ) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val title = if (status == ReminderStatus.OVERDUE) {
            context.getString(R.string.reminder_overdue_title, taskName)
        } else {
            context.getString(R.string.reminder_upcoming_title, taskName)
        }

        val pendingIntent = if (vehicleId != MainActivity.NO_VEHICLE_ID_EXTRA) {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_REMINDERS
                putExtra(MainActivity.EXTRA_VEHICLE_ID, vehicleId)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else null

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(title)
            .setContentText(vehicleLabel)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (pendingIntent != null) {
            notificationBuilder.setContentIntent(pendingIntent)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun cancel(reminderId: Long) {
        notificationManager.cancel(reminderId.toInt())
    }

    fun notifyTest(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return false

        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(context.getString(R.string.test_notification_title))
            .setContentText(context.getString(R.string.test_notification_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(TEST_NOTIFICATION_ID, notification)
        return true
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        private const val TEST_NOTIFICATION_ID = 9999
    }
}
