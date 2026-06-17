package com.innogen.aipro.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.innogen.aipro.presentation.auth.AuthScreen
import com.innogen.aipro.presentation.dashboard.DashboardScreen
import com.innogen.aipro.presentation.generation.GenerationScreen
import com.innogen.aipro.presentation.project.ProjectOverviewScreen
import com.innogen.aipro.presentation.codeviewer.CodeViewerScreen
import com.innogen.aipro.presentation.github.GitHubScreen
import com.innogen.aipro.presentation.deployment.DeploymentScreen
import com.innogen.aipro.presentation.profile.ProfileScreen
import com.innogen.aipro.presentation.onboarding.OnboardingScreen
import com.innogen.aipro.presentation.splash.SplashScreen

/** All navigation routes in the app */
object Routes {
    const val SPLASH      = "splash"
    const val ONBOARDING  = "onboarding"
    const val AUTH        = "auth"
    const val DASHBOARD   = "dashboard"
    const val GENERATION  = "generation/{prompt}"
    const val PROJECT     = "project/{projectId}"
    const val CODE_VIEWER = "code_viewer/{projectId}"
    const val GITHUB      = "github/{projectId}"
    const val DEPLOYMENT  = "deployment/{projectId}"
    const val PROFILE     = "profile"

    fun generation(prompt: String)  = "generation/$prompt"
    fun project(projectId: String)  = "project/$projectId"
    fun codeViewer(projectId: String) = "code_viewer/$projectId"
    fun github(projectId: String)   = "github/$projectId"
    fun deployment(projectId: String) = "deployment/$projectId"
}

/** Slide-in/out transitions used across all screens */
private fun enterTransition() = slideInHorizontally(
    initialOffsetX = { it },
    animationSpec   = tween(300)
) + fadeIn(animationSpec = tween(300))

private fun exitTransition() = slideOutHorizontally(
    targetOffsetX = { -it },
    animationSpec  = tween(300)
) + fadeOut(animationSpec = tween(300))

private fun popEnterTransition() = slideInHorizontally(
    initialOffsetX = { -it },
    animationSpec   = tween(300)
) + fadeIn(animationSpec = tween(300))

private fun popExitTransition() = slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec  = tween(300)
) + fadeOut(animationSpec = tween(300))

@Composable
fun InnoGenNavHost() {
    val navController = rememberNavController()

    // Decide start destination based on auth state
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Routes.DASHBOARD
    } else {
        Routes.SPLASH
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination,
        enterTransition  = { enterTransition() },
        exitTransition   = { exitTransition() },
        popEnterTransition = { popEnterTransition() },
        popExitTransition  = { popExitTransition() }
    ) {
        // Splash
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onGetStarted = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // Auth
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        // Dashboard
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onGenerateApp = { prompt ->
                    navController.navigate(Routes.generation(prompt))
                },
                onOpenProject = { projectId ->
                    navController.navigate(Routes.project(projectId))
                },
                onOpenProfile = {
                    navController.navigate(Routes.PROFILE)
                }
            )
        }

        // AI Generation
        composable(
            route     = Routes.GENERATION,
            arguments = listOf(navArgument("prompt") { type = NavType.StringType })
        ) { backStack ->
            val prompt = backStack.arguments?.getString("prompt") ?: ""
            GenerationScreen(
                prompt = prompt,
                onGenerationComplete = { projectId ->
                    navController.navigate(Routes.project(projectId)) {
                        popUpTo(Routes.DASHBOARD)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Project Overview
        composable(
            route     = Routes.PROJECT,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStack ->
            val projectId = backStack.arguments?.getString("projectId") ?: ""
            ProjectOverviewScreen(
                projectId   = projectId,
                onViewCode  = { navController.navigate(Routes.codeViewer(projectId)) },
                onDeploy    = { navController.navigate(Routes.deployment(projectId)) },
                onGitHub    = { navController.navigate(Routes.github(projectId)) },
                onBack      = { navController.popBackStack() }
            )
        }

        // Code Viewer
        composable(
            route     = Routes.CODE_VIEWER,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStack ->
            val projectId = backStack.arguments?.getString("projectId") ?: ""
            CodeViewerScreen(
                projectId = projectId,
                onBack    = { navController.popBackStack() }
            )
        }

        // GitHub
        composable(
            route     = Routes.GITHUB,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStack ->
            val projectId = backStack.arguments?.getString("projectId") ?: ""
            GitHubScreen(
                projectId = projectId,
                onBack    = { navController.popBackStack() }
            )
        }

        // Deployment
        composable(
            route     = Routes.DEPLOYMENT,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStack ->
            val projectId = backStack.arguments?.getString("projectId") ?: ""
            DeploymentScreen(
                projectId = projectId,
                onBack    = { navController.popBackStack() }
            )
        }

        // Profile
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }
    }
}
