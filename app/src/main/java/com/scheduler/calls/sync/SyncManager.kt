package com.scheduler.calls.sync

import android.content.Context
import android.util.Log
import com.scheduler.calls.alarm.CallScheduler
import com.scheduler.calls.data.CallRepository
import com.scheduler.calls.data.ScheduledCall

class SyncManager(
    private val appContext: Context,
    private val repo: CallRepository,
    private val settings: SyncSettings
) {

    sealed interface Result {
        data class Ok(val pulled: Int, val pushed: Int) : Result
        data class Error(val message: String) : Result
        data object NotConfigured : Result
    }

    suspend fun syncOnce(): Result {
        val baseUrl = settings.serverUrl
        if (baseUrl.isBlank()) return Result.NotConfigured

        val api = SyncClient.api(baseUrl)
        val since = settings.lastSyncMs
        val locallyDirty = repo.getModifiedSince(since)

        return try {
            val response = api.sync(
                SyncRequestDto(
                    since = since,
                    changes = locallyDirty.map { it.toDto() }
                )
            )

            applyServerChanges(response.changes)

            settings.lastSyncMs = response.serverTime
            settings.lastSyncStatus = "OK"
            Result.Ok(pulled = response.changes.size, pushed = locallyDirty.size)
        } catch (t: Throwable) {
            Log.w(TAG, "Sync failed", t)
            settings.lastSyncStatus = "Error: ${t.message ?: t.javaClass.simpleName}"
            Result.Error(t.message ?: t.javaClass.simpleName)
        }
    }

    private suspend fun applyServerChanges(changes: List<CallDto>) {
        val now = System.currentTimeMillis()
        for (dto in changes) {
            val existing = repo.getBySyncId(dto.syncId)
            if (existing != null && dto.updatedAt <= existing.updatedAt) continue

            val merged = dto.toEntity(existingId = existing?.id ?: 0)
            val savedId = repo.applyFromSync(merged)
            val saved = merged.copy(id = savedId)

            CallScheduler.cancel(appContext, savedId)
            if (shouldArm(saved, now)) {
                CallScheduler.schedule(appContext, savedId, saved.scheduledTimeMillis)
            }
        }
    }

    private fun shouldArm(call: ScheduledCall, now: Long): Boolean =
        !call.tombstone && !call.triggered && call.scheduledTimeMillis > now

    companion object {
        private const val TAG = "SyncManager"
    }
}
