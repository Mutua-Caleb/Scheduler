package com.scheduler.calls.data

import kotlinx.coroutines.flow.Flow

class CallRepository(private val dao: ScheduledCallDao) {

    fun observeAll(): Flow<List<ScheduledCall>> = dao.observeAll()

    suspend fun getById(id: Long): ScheduledCall? = dao.getById(id)

    suspend fun getUpcoming(now: Long): List<ScheduledCall> = dao.getUpcoming(now)

    suspend fun upsert(call: ScheduledCall): Long =
        if (call.id == 0L) dao.insert(call) else { dao.update(call); call.id }

    suspend fun delete(call: ScheduledCall) = dao.delete(call)

    suspend fun markTriggered(id: Long) = dao.markTriggered(id)
}
