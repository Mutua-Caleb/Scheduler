package com.scheduler.calls.data

import kotlinx.coroutines.flow.Flow

class CallRepository(
    private val callDao: ScheduledCallDao,
    private val eventDao: CallEventDao
) {

    fun observeAll(): Flow<List<ScheduledCall>> = callDao.observeAll()

    fun observeHistory(): Flow<List<CallEvent>> = eventDao.observeRecent()

    suspend fun getById(id: Long): ScheduledCall? = callDao.getById(id)

    suspend fun getBySyncId(syncId: String): ScheduledCall? = callDao.getBySyncId(syncId)

    suspend fun getUpcoming(now: Long): List<ScheduledCall> = callDao.getUpcoming(now)

    suspend fun getModifiedSince(since: Long): List<ScheduledCall> =
        callDao.getModifiedSince(since)

    suspend fun upsert(call: ScheduledCall): Long {
        val stamped = call.copy(updatedAt = System.currentTimeMillis())
        return if (stamped.id == 0L) callDao.insert(stamped) else {
            callDao.update(stamped); stamped.id
        }
    }

    /** Apply an inbound sync row verbatim (no auto-stamp; preserves server updatedAt). */
    suspend fun applyFromSync(call: ScheduledCall): Long =
        if (call.id == 0L) callDao.insert(call) else { callDao.update(call); call.id }

    suspend fun softDelete(call: ScheduledCall) {
        callDao.update(
            call.copy(
                tombstone = true,
                triggered = true,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markTriggered(id: Long) = callDao.markTriggered(id)

    suspend fun update(call: ScheduledCall) {
        callDao.update(call.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun logEvent(event: CallEvent) { eventDao.insert(event) }
}
