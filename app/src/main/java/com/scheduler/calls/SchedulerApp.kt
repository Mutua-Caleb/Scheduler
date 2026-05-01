package com.scheduler.calls

import android.app.Application
import com.scheduler.calls.data.AppDatabase
import com.scheduler.calls.data.CallRepository

class SchedulerApp : Application() {

    val repository: CallRepository by lazy {
        val db = AppDatabase.get(this)
        CallRepository(db.scheduledCallDao(), db.callEventDao())
    }
}
