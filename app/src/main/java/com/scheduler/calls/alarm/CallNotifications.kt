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
    const val CHANNEL_RESULT = "call_result_prompt"

    private const val CONFIRM_NOTIFICATION_ID_BASE = 10_000
    private const val RESULT_NOTIFICATION_ID_BASE = 20_000

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
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RESULT,
                context.getString(R.string.channel_result_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.channel_result_description) }
        )
    }

    fun confirmNotificationId(callId: Long): Int =
        CONFIRM_NOTIFICATION_ID_BASE + callId.toInt()

    fun resultNotificationId(eventId: Long): Int =
        RESULT_NOTIFICATION_ID_BASE + eventId.toInt()

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

    fun showResultPrompt(
        context: Context,
        eventId: Long,
        contactDisplay: String
    ) {
        ensureChannels(context)

        fun resultPi(result: com.scheduler.calls.data.CallResult): PendingIntent {
            val intent = Intent(context, CallResultReceiver::class.java).apply {
                putExtra(CallResultReceiver.EXTRA_EVENT_ID, eventId)
                putExtra(CallResultReceiver.EXTRA_RESULT, result.name)
            }
            val req = (eventId.toInt() shl 4) xor result.name.hashCode()
            return PendingIntent.getBroadcast(
                context, req, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val openActivity = Intent(
            context,
            com.scheduler.calls.ui.CallResultActivity::class.java
        ).apply {
            putExtra(CallResultReceiver.EXTRA_EVENT_ID, eventId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val openPi = PendingIntent.getActivity(
            context, eventId.toInt(), openActivity,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.result_prompt_title, contactDisplay)
        val body = context.getString(R.string.result_prompt_body)

        val notification = NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setTimeoutAfter(30 * 60 * 1000L)
            .setContentIntent(openPi)
            .addAction(
                0,
                context.getString(R.string.result_reached),
                resultPi(com.scheduler.calls.data.CallResult.REACHED)
            )
            .addAction(
                0,
                context.getString(R.string.result_no_answer),
                resultPi(com.scheduler.calls.data.CallResult.NO_ANSWER)
            )
            .addAction(
                0,
                context.getString(R.string.result_voicemail),
                resultPi(com.scheduler.calls.data.CallResult.VOICEMAIL)
            )
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(resultNotificationId(eventId), notification)
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
