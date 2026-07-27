package com.yy.medtrace.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.repository.MemberRepository
import com.yy.medtrace.data.repository.RecordRepository
import com.yy.medtrace.data.repository.TodoRepository
import com.yy.medtrace.ui.screens.*
import com.yy.medtrace.viewmodel.*
import androidx.lifecycle.viewmodel.compose.viewModel

private const val ANIMATION_DURATION = 150

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(
    navController: NavHostController,
    database: AppDatabase,
    memberRepository: MemberRepository,
    recordRepository: RecordRepository,
    todoRepository: TodoRepository
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
            PrivacyConsentScreen(
                navController = navController,
                onDecline = { /* activity?.finish() */ }
            )
        }
        composable("onboarding") {
            OnboardingScreen(navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                memberRepository = memberRepository,
                todoRepository = todoRepository,
                recordRepository = recordRepository
            )
        }
        composable(Screen.Family.route) {
            FamilyScreen(database, navController, recordRepository)
        }
        composable(Screen.Reminders.route) {
            val viewModel: RemindersViewModel = viewModel(factory = RemindersViewModelFactory(database))
            RemindersScreen(viewModel, navController)
        }
        composable(Screen.Profile.route) {
            val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(database))
            ProfileScreen(viewModel, navController)
        }
        composable("medical_records") {
            MedicalRecordScreen(database, navController)
        }
        composable("trends") {
            TrendsScreen(database, navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(database, navController)
        }
        composable("add_record") {
            AddMedicalRecordScreen(database, navController)
        }
        composable(
            "add_record/{recordId}",
            arguments = listOf(navArgument("recordId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("recordId")?.toLongOrNull() ?: -1L
            AddMedicalRecordScreen(database, navController, recordId = id)
        }
        composable(
            "member_detail/{memberId}",
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("memberId")?.toLongOrNull() ?: -1L
            MemberDetailScreen(database, navController, memberId = id)
        }
        composable("privacy_policy") {
            PrivacyPolicyScreen(navController)
        }
        composable("user_agreement") {
            UserAgreementScreen(navController)
        }
    }
}