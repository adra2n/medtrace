package com.yy.medtrace.navigation

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yy.medtrace.ui.screens.*
import com.yy.medtrace.viewmodel.*

private const val ANIMATION_DURATION = 150

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        enterTransition = { fadeIn(animationSpec = tween(ANIMATION_DURATION)) },
        exitTransition = { fadeOut(animationSpec = tween(ANIMATION_DURATION)) },
        popEnterTransition = { fadeIn(animationSpec = tween(ANIMATION_DURATION)) },
        popExitTransition = { fadeOut(animationSpec = tween(ANIMATION_DURATION)) }
    ) {
        composable("splash") {
            SplashScreen(navController)
        }
        composable("privacy_consent") {
            val activity = LocalContext.current as? Activity
            PrivacyConsentScreen(
                navController = navController,
                onDecline = { activity?.finish() }
            )
        }
        composable("onboarding") {
            OnboardingScreen(navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Family.route) {
            val viewModel: FamilyViewModel = hiltViewModel()
            FamilyScreen(viewModel, navController)
        }
        composable(Screen.Reminders.route) {
            RemindersScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable("medical_records") {
            val viewModel: MedicalRecordViewModel = hiltViewModel()
            MedicalRecordScreen(viewModel, navController)
        }
        composable("trends") {
            val viewModel: TrendsViewModel = hiltViewModel()
            TrendsScreen(viewModel, navController)
        }
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(viewModel, navController)
        }
        composable("add_record") {
            val viewModel: AddMedicalRecordViewModel = hiltViewModel()
            AddMedicalRecordScreen(viewModel, navController)
        }
        composable(
            "add_record/{recordId}",
            arguments = listOf(navArgument("recordId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("recordId")?.toLongOrNull() ?: -1L
            val viewModel: AddMedicalRecordViewModel = hiltViewModel()
            AddMedicalRecordScreen(viewModel, navController, recordId = id)
        }
        composable(
            "member_detail/{memberId}",
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("memberId")?.toLongOrNull() ?: -1L
            val viewModel: MemberDetailViewModel = hiltViewModel()
            MemberDetailScreen(viewModel, navController, memberId = id)
        }
        composable("privacy_policy") {
            PrivacyPolicyScreen(navController)
        }
        composable("user_agreement") {
            UserAgreementScreen(navController)
        }
    }
}
