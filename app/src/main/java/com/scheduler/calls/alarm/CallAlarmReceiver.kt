package com.scheduler.calls.alarm

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.scheduler.calls.data.AppDatabase
import com.scheduler.calls.overlay.NotesOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getLongExtra(CallScheduler.EXTRA_CALL_ID, -1L)
        if (callId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context).scheduledCallDao()
                val call = dao.getById(callId) ?: return@launch
                dao.markTriggered(call.id)

                NotesOverlayService.start(context, call.contactName, call.notes)
                placeCall(context, call.phoneNumber)
            } catch (t: Throwable) {
                Log.e("CallAlarmReceiver", "Failed firing scheduled call", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun placeCall(context: Context, phoneNumber: String) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = Intent(
            if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL,
            Uri.fromParts("tel", phoneNumber, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
