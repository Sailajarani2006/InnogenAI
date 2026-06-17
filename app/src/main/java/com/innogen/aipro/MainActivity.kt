package com.innogen.aipro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.innogen.aipro.presentation.navigation.InnoGenNavHost
import com.innogen.aipro.presentation.theme.InnoGenTheme
import com.innogen.aipro.presentation.profile.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity - Single Activity Architecture with Jetpack Compose
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            val isDarkMode by profileViewModel.isDarkMode.collectAsState(initial = false)

            InnoGenTheme(darkTheme = isDarkMode) {
                InnoGenNavHost()
            }
        }
    }
}
