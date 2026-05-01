package com.scheduler.calls.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.scheduler.calls.R
import com.scheduler.calls.data.ScheduledCall

object CallNotifications {

    const val CHANNEL_CONFIRM = "call_confirm"
    const val CHANNEL_NOTES = "call_notes_overlay"

    private const val CONFIRM_NOTIFICATION_ID_BASE = 10_000

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CONFIRM,
                context.getString(R.string.channel_confirm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.channel_confirm_description) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NOTES,
                context.getString(R.string.channel_notes_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.channel_notes_description) }
        )
    }

    fun confirmNotificationId(callId: Long): Int =
        CONFIRM_NOTIFICATION_ID_BASE + callId.toInt()

    fun showConfirmation(context: Context, call: ScheduledCall, autoPlaceAtMillis: Long) {
        ensureChannels(context)
        val callNowPi = actionPendingIntent(context, call.id, CallActionReceiver.ACTION_CALL_NOW)
        val snoozePi = actionPendingIntent(context, call.id, CallActionReceiver.ACTION_SNOOZE)
        val cancelPi = actionPendingIntent(context, call.id, CallActionReceiver.ACTION_CANCEL)

        val displayName = call.contactName.ifBlank { call.phoneNumber }
        val title = context.getString(R.string.confirm_title, displayName)
        val autoLine = context.getString(R.string.confirm_auto_dial)
        val bigText = buildString {
            append(autoLine)
            if (call.notes.isNotBlank()) {
                append("\n\n")
                append(call.notes)
            }
        }

        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_CONFIRM)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(title)
            .setContentText(if (call.notes.isBlank()) autoLine else call.notes)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setWhen(autoPlaceAtMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setFullScreenIntent(callNowPi, true)
            .addAction(
                android.R.drawable.ic_menu_call,
                context.getString(R.string.action_call_now),
                callNowPi
            )
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                context.getString(R.string.action_snooze),
                snoozePi
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.action_cancel),
                cancelPi
            )
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(confirmNotificationId(call.id), notification)
    }

    fun cancelConfirmation(context: Context, callId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(confirmNotificationId(callId))
    }

    private fun actionPendingIntent(context: Context, callId: Long, action: String): PendingIntent {
        val intent = Intent(context, CallActionReceiver::class.java).apply {
            this.action = action
            putExtra(CallScheduler.EXTRA_CALL_ID, callId)
        }
        val req = (callId.toInt() shl 4) xor action.hashCode()
        return PendingIntent.getBroadcast(
            context,
            req,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
