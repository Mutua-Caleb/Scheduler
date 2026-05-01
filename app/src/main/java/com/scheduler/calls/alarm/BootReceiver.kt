package com.scheduler.calls.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.scheduler.calls.data.AppDatabase
import com.scheduler.calls.data.ScheduledCallTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val dao = AppDatabase.get(context).scheduledCallDao()
                dao.getUpcoming(now).forEach { call ->
                    // Recompute the wall-clock time in the saved zone in case DST shifted
                    // while we were powered off. Only update if the new instant is still
                    // in the future — otherwise leave the stored value alone so we don't
                    // silently bury a missed fire.
                    val recomputed = ScheduledCallTime.computeMillis(
                        call.parsedLocalDateTime(),
                        call.zone()
                    )
                    val target = if (recomputed > now) recomputed else call.scheduledTimeMillis
                    if (target != call.scheduledTimeMillis) {
                        dao.update(call.copy(scheduledTimeMillis = target))
                    }
                    if (target > now) {
                        CallScheduler.schedule(context, call.id, target)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
