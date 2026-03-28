package com.example.pythonspike

import android.app.Application

class PythonSpikeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PythonPrewarmer.schedule(this)
    }
}
