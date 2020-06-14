/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.devbyteviewer

import android.app.Application
import android.os.Build
import androidx.work.*
import com.example.android.devbyteviewer.work.RefreshDataWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Override application to setup background work via WorkManager
 */
class DevByteApplication : Application() {

    //Best Practice: The onCreate() method runs in the main thread.
    // Performing a long-running operation in onCreate() might block the UI thread
    // and cause a delay in loading the app. To avoid this problem, run tasks such
    // as initializing Timber and scheduling WorkManager off the main thread, inside a coroutine.
    //delayedInit() to start a coroutine.
    private val applicationScope = CoroutineScope(Dispatchers.Default)

    private fun delayedInit() {
        applicationScope.launch {
            Timber.plant(Timber.DebugTree())
            setupRecurringWork()
        }
    }

    /**
     * onCreate is called before the first screen is shown to the user.
     * Use it to setup any background tasks, running expensive setup operations in a background
     * thread to avoid delaying app start.
     */
    override fun onCreate() {
        super.onCreate()
        delayedInit()
    }

    /**
     * Setup WorkManager background job to 'fetch' new network data daily.
     * The first execution happens immediately, or as soon as the given constraints are met.
     */
    private fun setupRecurringWork() {

        //specify any constraints to avoid the work if the device is low
        // on battery, sleeping, or has no network connection , idle etc.
        val constrains = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(true)
                .apply {
                    //This constraint runs the work request only when the user isn't actively using the device.
                    // This feature is only available in Android 6.0 (Marshmallow) and higher,
                    // so add a condition for SDK version M and higher.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        setRequiresDeviceIdle(true)
                    }
                }
                .build()

        //Periodic work request to run once a day with the specific conditions (constraints)
        val repeatingRequest = PeriodicWorkRequestBuilder<RefreshDataWorker>(1, TimeUnit.DAYS)
                .setConstraints(constrains)
                .build()

        //JUST FYI:
        // val repeatingRequest = PeriodicWorkRequestBuilder<RefreshDataWorker>(15, TimeUnit.MINUTES).build()

        //Schedule a WorkRequest with WorkManager
        //If pending (uncompleted) work exists with the same name,
        // the ExistingPeriodicWorkPolicy.KEEP parameter makes the WorkManager
        // keep the previous periodic work and discard the new work request.
        WorkManager.getInstance().enqueueUniquePeriodicWork(
                RefreshDataWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                repeatingRequest
        )
    }
}
