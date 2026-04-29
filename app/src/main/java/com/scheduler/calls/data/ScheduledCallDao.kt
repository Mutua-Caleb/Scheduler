package com.scheduler.calls.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledCallDao {

    @Query("SELECT * FROM scheduled_calls ORDER BY scheduledTimeMillis ASC")
    fun observeAll(): Flow<List<ScheduledCall>>

    @Query("SELECT * FROM scheduled_calls WHERE id = :id")
    suspend fun getById(id: Long): ScheduledCall?

    @Query("SELECT * FROM scheduled_calls WHERE triggered = 0 AND scheduledTimeMillis > :now")
    suspend fun getUpcoming(now: Long): List<ScheduledCall>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: ScheduledCall): Long

    @Update
    suspend fun update(call: ScheduledCall)

    @Delete
    suspend fun delete(call: ScheduledCall)

    @Query("UPDATE scheduled_calls SET triggered = 1 WHERE id = :id")
    suspend fun markTriggered(id: Long)
}
