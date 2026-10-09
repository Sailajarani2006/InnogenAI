package com.innogen.aipro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.innogen.aipro.presentation.navigation.InnoGenNavHost
import com.innogen.aipro.presentation.theme.InnoGenTheme
import com.innogen.aipro.presentation.profile.ProfileViewModel
import com.innogen.aipro.presentation.github.GitHubViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val profileViewModel: ProfileViewModel by viewModels()
    private val gitHubViewModel : GitHubViewModel  by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Handle deep link if launched from GitHub OAuth
        handleIntent(intent)

        setContent {
            val isDarkMode by profileViewModel.isDarkMode.collectAsState(initial = false)
            InnoGenTheme(darkTheme = isDarkMode) {
                InnoGenNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    /**
     * Handles the GitHub OAuth callback deep link:
     * innogenai://callback?code=GITHUB_CODE&state=innogen_secure_state
     */
    private fun handleIntent(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme == "innogenai" && data.host == "callback") {
            val code  = data.getQueryParameter("code")
            val state = data.getQueryParameter("state")
            // Validate state to prevent CSRF
            if (code != null && state == "innogen_secure_state") {
                gitHubViewModel.handleOAuthCallback(code)
            }
        }
    }
}
