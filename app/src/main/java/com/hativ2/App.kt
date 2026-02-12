package com.hativ2

import android.app.Application
import com.hativ2.data.AppDatabase

@dagger.hilt.android.HiltAndroidApp
class App : Application() {
    // Database initialization is now handled by Hilt
    
    override fun onCreate() {
        super.onCreate()
    }
}
