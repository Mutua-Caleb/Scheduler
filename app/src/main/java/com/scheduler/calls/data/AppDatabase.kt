package com.scheduler.calls.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun fromRecurrence(value: Recurrence): String = value.name
    @TypeConverter fun toRecurrence(value: String): Recurrence =
        runCatching { Recurrence.valueOf(value) }.getOrDefault(Recurrence.NONE)

    @TypeConverter fun fromOutcome(value: CallOutcome): String = value.name
    @TypeConverter fun toOutcome(value: String): CallOutcome =
        runCatching { CallOutcome.valueOf(value) }.getOrDefault(CallOutcome.CALLED)

    @TypeConverter fun fromResult(value: CallResult?): String? = value?.name
    @TypeConverter fun toResult(value: String?): CallResult? = value?.let {
        runCatching { CallResult.valueOf(it) }.getOrNull()
    }
}

@Database(
    entities = [ScheduledCall::class, CallEvent::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scheduledCallDao(): ScheduledCallDao
    abstract fun callEventDao(): CallEventDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scheduler.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
