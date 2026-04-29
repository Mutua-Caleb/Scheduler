package com.scheduler.calls.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.scheduler.calls.data.AppDatabase
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
                    CallScheduler.schedule(context, call.id, call.scheduledTimeMillis)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
