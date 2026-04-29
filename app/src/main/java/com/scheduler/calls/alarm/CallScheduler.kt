package com.scheduler.calls.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object CallScheduler {

    private const val ACTION_FIRE_CALL = "com.scheduler.calls.FIRE_CALL"
    const val EXTRA_CALL_ID = "call_id"

    fun schedule(context: Context, callId: Long, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, callId)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun cancel(context: Context, callId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, callId))
    }

    private fun pendingIntent(context: Context, callId: Long): PendingIntent {
        val intent = Intent(context, CallAlarmReceiver::class.java).apply {
            action = ACTION_FIRE_CALL
            putExtra(EXTRA_CALL_ID, callId)
        }
        return PendingIntent.getBroadcast(
            context,
            callId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
