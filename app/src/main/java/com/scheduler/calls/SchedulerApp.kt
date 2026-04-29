package com.scheduler.calls

import android.app.Application
import com.scheduler.calls.data.AppDatabase
import com.scheduler.calls.data.CallRepository

class SchedulerApp : Application() {

    val repository: CallRepository by lazy {
        CallRepository(AppDatabase.get(this).scheduledCallDao())
    }
}
