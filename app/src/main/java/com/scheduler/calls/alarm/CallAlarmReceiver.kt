package com.scheduler.calls.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.scheduler.calls.data.AppDatabase
import com.scheduler.calls.data.CallOutcome
import com.scheduler.calls.data.CallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getLongExtra(CallScheduler.EXTRA_CALL_ID, -1L)
        if (callId < 0) return
        val action = intent.action ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(context)
                val repo = CallRepository(db.scheduledCallDao(), db.callEventDao())
                val call = db.scheduledCallDao().getById(callId) ?: return@launch

                when (action) {
                    CallScheduler.ACTION_FIRE_CALL -> {
                        val autoAt = System.currentTimeMillis() + AUTO_PLACE_DELAY_MS
                        CallNotifications.showConfirmation(context, call, autoAt)
                        CallScheduler.scheduleAutoPlace(context, call.id, autoAt)
                    }
                    CallScheduler.ACTION_AUTO_PLACE -> {
                        CallExecutor.execute(context, call, repo, CallOutcome.AUTO_FIRED)
                    }
                }
            } catch (t: Throwable) {
                Log.e("CallAlarmReceiver", "Failed handling alarm: ${intent.action}", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val AUTO_PLACE_DELAY_MS = 30_000L
    }
}
