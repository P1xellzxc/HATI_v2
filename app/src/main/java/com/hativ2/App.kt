package com.hativ2

import android.app.Application
import com.hativ2.data.AppDatabase

class App : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
    }
}
