package com.scheduler.calls.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scheduler.calls.SchedulerApp
import com.scheduler.calls.alarm.CallScheduler
import com.scheduler.calls.data.CallRepository
import com.scheduler.calls.data.ScheduledCall
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CallsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: CallRepository = (app as SchedulerApp).repository

    val calls: StateFlow<List<ScheduledCall>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveCall(
        existingId: Long,
        name: String,
        phone: String,
        timeMillis: Long,
        notes: String
    ) {
        viewModelScope.launch {
            val call = ScheduledCall(
                id = existingId,
                contactName = name.trim(),
                phoneNumber = phone.trim(),
                scheduledTimeMillis = timeMillis,
                notes = notes.trim(),
                triggered = false
            )
            val id = repo.upsert(call)
            CallScheduler.cancel(getApplication(), id)
            if (timeMillis > System.currentTimeMillis()) {
                CallScheduler.schedule(getApplication(), id, timeMillis)
            }
        }
    }

    fun delete(call: ScheduledCall) {
        viewModelScope.launch {
            CallScheduler.cancel(getApplication(), call.id)
            repo.delete(call)
        }
    }
}
