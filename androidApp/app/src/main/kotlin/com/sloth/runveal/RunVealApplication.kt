package com.sloth.runveal

import android.app.Application
import org.maplibre.android.MapLibre

class RunVealApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
