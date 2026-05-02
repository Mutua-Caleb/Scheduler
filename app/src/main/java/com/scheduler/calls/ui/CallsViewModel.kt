package com.scheduler.calls.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scheduler.calls.SchedulerApp
import com.scheduler.calls.alarm.CallScheduler
import com.scheduler.calls.data.CallEvent
import com.scheduler.calls.data.CallRepository
import com.scheduler.calls.data.Recurrence
import com.scheduler.calls.data.ScheduledCall
import com.scheduler.calls.data.ScheduledCallTime
import com.scheduler.calls.sync.SyncManager
import com.scheduler.calls.sync.SyncSettings
import com.scheduler.calls.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

class CallsViewModel(app: Application) : AndroidViewModel(app) {

    private val schedulerApp = app as SchedulerApp
    private val repo: CallRepository = schedulerApp.repository
    private val syncManager: SyncManager = schedulerApp.syncManager
    val syncSettings: SyncSettings = schedulerApp.syncSettings

    val calls: StateFlow<List<ScheduledCall>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<CallEvent>> =
        repo.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    init {
        // Try a one-shot sync on launch so the list is fresh when the user opens it.
        if (syncSettings.isConfigured) {
            SyncWorker.enqueueOneShot(getApplication())
        }
    }

    fun saveCall(
        existingId: Long,
        name: String,
        phone: String,
        localDateTime: LocalDateTime,
        zoneId: ZoneId,
        notes: String,
        recurrence: Recurrence
    ) {
        viewModelScope.launch {
            val existing = if (existingId != 0L) repo.getById(existingId) else null
            val newMillis = ScheduledCallTime.computeMillis(localDateTime, zoneId)
            val timeIsFuture = newMillis > System.currentTimeMillis()
            val timeChanged = existing?.scheduledTimeMillis != newMillis

            val newTriggered = when {
                existing == null -> false
                timeChanged && timeIsFuture -> false
                else -> existing.triggered
            }

            val call = (existing ?: ScheduledCall(
                contactName = "",
                phoneNumber = "",
                localDateTime = "",
                zoneId = zoneId.id,
                scheduledTimeMillis = 0L,
                notes = ""
            )).copy(
                contactName = name.trim(),
                phoneNumber = phone.trim(),
                localDateTime = localDateTime.toString(),
                zoneId = zoneId.id,
                scheduledTimeMillis = newMillis,
                notes = notes.trim(),
                triggered = newTriggered,
                recurrence = recurrence,
                tombstone = false
            )
            val id = repo.upsert(call)
            CallScheduler.cancel(getApplication(), id)
            if (timeIsFuture && !newTriggered) {
                CallScheduler.schedule(getApplication(), id, newMillis)
            }
            if (syncSettings.isConfigured) {
                SyncWorker.enqueueOneShot(getApplication())
            }
        }
    }

    fun delete(call: ScheduledCall) {
        viewModelScope.launch {
            CallScheduler.cancel(getApplication(), call.id)
            repo.softDelete(call)
            if (syncSettings.isConfigured) {
                SyncWorker.enqueueOneShot(getApplication())
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _syncing.value = true
            _syncMessage.value = ""
            val result = syncManager.syncOnce()
            _syncMessage.value = when (result) {
                is SyncManager.Result.Ok ->
                    "Synced (pulled ${result.pulled}, pushed ${result.pushed})"
                is SyncManager.Result.NotConfigured -> "Server URL not set"
                is SyncManager.Result.Error -> result.message
            }
            _syncing.value = false
        }
    }

    fun setServerUrl(url: String) {
        syncSettings.serverUrl = url
        if (syncSettings.isConfigured) {
            SyncWorker.enqueuePeriodic(getApplication())
        }
    }
}
