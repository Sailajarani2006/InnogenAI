package com.innogen.aipro

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

/**
 * InnoGenAIPro Application class
 * Initializes Hilt DI and Firebase
 */
@HiltAndroidApp
class InnoGenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
