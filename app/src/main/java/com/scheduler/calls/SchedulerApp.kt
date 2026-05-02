package com.scheduler.calls

import android.app.Application
import com.scheduler.calls.data.AppDatabase
import com.scheduler.calls.data.CallRepository
import com.scheduler.calls.sync.SyncManager
import com.scheduler.calls.sync.SyncSettings
import com.scheduler.calls.sync.SyncWorker

class SchedulerApp : Application() {

    val repository: CallRepository by lazy {
        val db = AppDatabase.get(this)
        CallRepository(db.scheduledCallDao(), db.callEventDao())
    }

    val syncSettings: SyncSettings by lazy { SyncSettings(this) }

    val syncManager: SyncManager by lazy {
        SyncManager(this, repository, syncSettings)
    }

    override fun onCreate() {
        super.onCreate()
        SyncWorker.enqueuePeriodic(this)
    }
}
