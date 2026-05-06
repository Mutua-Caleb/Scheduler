package com.scheduler.calls.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallEventDao {

    @Query("SELECT * FROM call_events ORDER BY firedAtMillis DESC LIMIT 200")
    fun observeRecent(): Flow<List<CallEvent>>

    @Query("SELECT * FROM call_events WHERE id = :id")
    suspend fun getById(id: Long): CallEvent?

    @Insert
    suspend fun insert(event: CallEvent): Long

    @Query(
        """
        UPDATE call_events
        SET callResult = :result, resultNotes = :notes, resultRecordedAtMillis = :recordedAt
        WHERE id = :id
        """
    )
    suspend fun updateResult(id: Long, result: CallResult, notes: String, recordedAt: Long)
}
