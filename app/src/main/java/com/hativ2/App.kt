package com.hativ2

import android.app.Application
import android.os.StrictMode
import com.hativ2.data.AppDatabase

@dagger.hilt.android.HiltAndroidApp
class App : Application() {
    // Database initialization is now handled by Hilt
    
    override fun onCreate() {
        super.onCreate()
        enableStrictModeInDebug()
    }

    private fun enableStrictModeInDebug() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build()
            )
        }
    }
}
