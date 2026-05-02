package com.scheduler.calls.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.scheduler.calls.data.AppDatabase
import com.scheduler.calls.data.CallEvent
import com.scheduler.calls.data.CallOutcome
import com.scheduler.calls.data.CallRepository
import com.scheduler.calls.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallActionReceiver : BroadcastReceiver() {

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
                    ACTION_CALL_NOW ->
                        CallExecutor.execute(context, call, repo, CallOutcome.CALLED)

                    ACTION_SNOOZE -> {
                        CallNotifications.cancelConfirmation(context, call.id)
                        CallScheduler.cancelAutoPlace(context, call.id)
                        val newTime = System.currentTimeMillis() + SNOOZE_MS
                        repo.update(call.copy(scheduledTimeMillis = newTime, triggered = false))
                        CallScheduler.schedule(context, call.id, newTime)
                        repo.logEvent(
                            CallEvent(
                                scheduledCallId = call.id,
                                contactName = call.contactName,
                                phoneNumber = call.phoneNumber,
                                firedAtMillis = System.currentTimeMillis(),
                                outcome = CallOutcome.SNOOZED,
                                notes = call.notes
                            )
                        )
                    }

                    ACTION_CANCEL -> {
                        CallNotifications.cancelConfirmation(context, call.id)
                        CallScheduler.cancelAutoPlace(context, call.id)
                        repo.markTriggered(call.id)
                        repo.logEvent(
                            CallEvent(
                                scheduledCallId = call.id,
                                contactName = call.contactName,
                                phoneNumber = call.phoneNumber,
                                firedAtMillis = System.currentTimeMillis(),
                                outcome = CallOutcome.CANCELLED,
                                notes = call.notes
                            )
                        )
                        CallExecutor.advanceRecurrence(context, call, repo)
                    }
                }
                SyncWorker.enqueueOneShot(context)
            } catch (t: Throwable) {
                Log.e("CallActionReceiver", "Failed handling action: $action", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CALL_NOW = "com.scheduler.calls.action.CALL_NOW"
        const val ACTION_SNOOZE = "com.scheduler.calls.action.SNOOZE"
        const val ACTION_CANCEL = "com.scheduler.calls.action.CANCEL"
        const val SNOOZE_MS = 10 * 60 * 1000L
    }
}
