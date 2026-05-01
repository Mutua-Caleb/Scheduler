package com.scheduler.calls.alarm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.scheduler.calls.data.CallEvent
import com.scheduler.calls.data.CallOutcome
import com.scheduler.calls.data.CallRepository
import com.scheduler.calls.data.ScheduledCall
import com.scheduler.calls.overlay.NotesOverlayService

object CallExecutor {

    suspend fun execute(
        context: Context,
        call: ScheduledCall,
        repo: CallRepository,
        outcome: CallOutcome
    ) {
        CallNotifications.cancelConfirmation(context, call.id)
        CallScheduler.cancelAutoPlace(context, call.id)

        NotesOverlayService.start(context, call.contactName, call.notes)
        placeCall(context, call.phoneNumber)

        repo.logEvent(
            CallEvent(
                scheduledCallId = call.id,
                contactName = call.contactName,
                phoneNumber = call.phoneNumber,
                firedAtMillis = System.currentTimeMillis(),
                outcome = outcome,
                notes = call.notes
            )
        )

        repo.markTriggered(call.id)
        advanceRecurrence(context, call, repo)
    }

    private fun placeCall(context: Context, phoneNumber: String) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = Intent(
            if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL,
            Uri.fromParts("tel", phoneNumber, null)
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    suspend fun advanceRecurrence(
        context: Context,
        call: ScheduledCall,
        repo: CallRepository
    ) {
        val next = call.recurrence.nextOccurrence(call.scheduledTimeMillis) ?: return
        val refreshed = call.copy(scheduledTimeMillis = next, triggered = false)
        repo.update(refreshed)
        CallScheduler.schedule(context, refreshed.id, next)
    }
}
