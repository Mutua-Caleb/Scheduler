package com.scheduler.calls.data

import kotlinx.coroutines.flow.Flow

class CallRepository(
    private val callDao: ScheduledCallDao,
    private val eventDao: CallEventDao
) {

    fun observeAll(): Flow<List<ScheduledCall>> = callDao.observeAll()

    fun observeHistory(): Flow<List<CallEvent>> = eventDao.observeRecent()

    suspend fun getById(id: Long): ScheduledCall? = callDao.getById(id)

    suspend fun getUpcoming(now: Long): List<ScheduledCall> = callDao.getUpcoming(now)

    suspend fun upsert(call: ScheduledCall): Long =
        if (call.id == 0L) callDao.insert(call) else { callDao.update(call); call.id }

    suspend fun delete(call: ScheduledCall) = callDao.delete(call)

    suspend fun markTriggered(id: Long) = callDao.markTriggered(id)

    suspend fun update(call: ScheduledCall) = callDao.update(call)

    suspend fun logEvent(event: CallEvent) { eventDao.insert(event) }
}
