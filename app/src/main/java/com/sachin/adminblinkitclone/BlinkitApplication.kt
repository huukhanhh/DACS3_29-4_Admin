package com.sachin.adminblinkitclone

import android.app.Application
import com.cloudinary.android.MediaManager

class BlinkitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = mapOf(
            "cloud_name" to "dcurxpdfs", // Thay bằng cloud_name của bạn
            "api_key" to "657681769612565",       // Thay bằng api_key của bạn
            "api_secret" to "COP16pCarH4uSMQwGR4iBSQ1RIk"  // Thay bằng api_secret của bạn
        )
        MediaManager.init(this, config)
    }
}