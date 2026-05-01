package com.scheduler.calls.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallEventDao {

    @Query("SELECT * FROM call_events ORDER BY firedAtMillis DESC LIMIT 200")
    fun observeRecent(): Flow<List<CallEvent>>

    @Insert
    suspend fun insert(event: CallEvent): Long
}
