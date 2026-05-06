package com.scheduler.calls.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.scheduler.calls.data.AppDatabase
import com.scheduler.calls.data.CallRepository
import com.scheduler.calls.data.CallResult
import com.scheduler.calls.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val resultName = intent.getStringExtra(EXTRA_RESULT) ?: return
        if (eventId < 0) return

        val result = runCatching { CallResult.valueOf(resultName) }.getOrNull() ?: return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(context)
                val repo = CallRepository(db.scheduledCallDao(), db.callEventDao())
                repo.setCallResult(eventId, result, notes = "")
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(CallNotifications.resultNotificationId(eventId))
                SyncWorker.enqueueOneShot(context)
            } catch (t: Throwable) {
                Log.e("CallResultReceiver", "Failed recording result", t)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_RESULT = "result"
    }
}
