package com.scheduler.calls.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object CallScheduler {

    const val ACTION_FIRE_CALL = "com.scheduler.calls.FIRE_CALL"
    const val ACTION_AUTO_PLACE = "com.scheduler.calls.AUTO_PLACE"
    const val EXTRA_CALL_ID = "call_id"

    private const val FIRE_REQ_OFFSET = 0
    private const val AUTO_REQ_OFFSET = 1_000_000

    fun schedule(context: Context, callId: Long, triggerAtMillis: Long) {
        setAlarm(context, triggerAtMillis, firePendingIntent(context, callId))
    }

    fun cancel(context: Context, callId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(firePendingIntent(context, callId))
        am.cancel(autoPlacePendingIntent(context, callId))
    }

    fun scheduleAutoPlace(context: Context, callId: Long, triggerAtMillis: Long) {
        setAlarm(context, triggerAtMillis, autoPlacePendingIntent(context, callId))
    }

    fun cancelAutoPlace(context: Context, callId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(autoPlacePendingIntent(context, callId))
    }

    private fun setAlarm(context: Context, triggerAtMillis: Long, pi: PendingIntent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun firePendingIntent(context: Context, callId: Long): PendingIntent {
        val intent = Intent(context, CallAlarmReceiver::class.java).apply {
            action = ACTION_FIRE_CALL
            putExtra(EXTRA_CALL_ID, callId)
        }
        return PendingIntent.getBroadcast(
            context,
            (callId.toInt() + FIRE_REQ_OFFSET),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun autoPlacePendingIntent(context: Context, callId: Long): PendingIntent {
        val intent = Intent(context, CallAlarmReceiver::class.java).apply {
            action = ACTION_AUTO_PLACE
            putExtra(EXTRA_CALL_ID, callId)
        }
        return PendingIntent.getBroadcast(
            context,
            (callId.toInt() + AUTO_REQ_OFFSET),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
