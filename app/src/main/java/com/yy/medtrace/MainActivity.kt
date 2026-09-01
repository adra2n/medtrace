package com.yy.medtrace

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import androidx.activity.compose.BackHandler
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.UserSettings
import com.yy.medtrace.data.settings.UserMode
import com.yy.medtrace.data.settings.UserModeStore
import com.yy.medtrace.navigation.Screen
import com.yy.medtrace.ui.screens.AddMedicalRecordScreen
import com.yy.medtrace.ui.screens.FamilyScreen
import com.yy.medtrace.ui.screens.HomeScreen
import com.yy.medtrace.ui.screens.MedicalRecordScreen
import com.yy.medtrace.ui.screens.MemberDetailScreen
import com.yy.medtrace.ui.screens.SettingsScreen
import com.yy.medtrace.ui.screens.RemindersScreen
import com.yy.medtrace.ui.screens.ProfileScreen
import com.yy.medtrace.ui.theme.ChiyaoleTheme
import com.yy.medtrace.viewmodel.AddMedicalRecordViewModel
import com.yy.medtrace.viewmodel.FamilyViewModel
import com.yy.medtrace.viewmodel.MedicalRecordViewModel
import com.yy.medtrace.viewmodel.MemberDetailViewModel
import com.yy.medtrace.viewmodel.SettingsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

// 纯 Compose 应用，无需 FragmentActivity（FragmentActivity 会额外拉起 fragment / appcompat 初始化，
// 拖慢冷启动并增大包体）。生物识别相关代码如需 FragmentActivity，见 BiometricHelper。
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)

        // 历史版本（v1–v5）因迁移缺失导致旧库被重置：提示用户从备份恢复。
        if (AppDatabase.migrationResetHappened) {
            Toast.makeText(
                this,
                "数据库已因版本升级重建，旧数据已清空。请到「设置 → 数据备份」从备份恢复。",
                Toast.LENGTH_LONG
            ).show()
        }

        // 初始化默认数据（默认设置 + 默认家庭成员「我自己」）
        lifecycleScope.launch(Dispatchers.IO) {
            if (database.userSettingsDao().getUserSettings().firstOrNull() == null) {
                database.userSettingsDao().insertOrUpdate(UserSettings())
            }
            if (database.familyMemberDao().getDefaultMember() == null) {
                database.familyMemberDao().insert(
                    FamilyMember.DEFAULT
                )
            }
        }

        // 处理从 SplashActivity 传来的导航参数
        val navigateTo = intent?.getStringExtra("navigate_to")

        setContent {
            val settings by database.userSettingsDao().getUserSettings().collectAsState(initial = null)
            ChiyaoleTheme(darkTheme = settings?.darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        initialRoute = navigateTo
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialRoute: String? = null
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val userModeStore = remember { UserModeStore(context) }
    val currentMode by userModeStore.currentMode.collectAsState()
    
    // 根据模式选择导航屏幕
    val screens = remember(currentMode) {
        when (currentMode) {
            UserMode.STANDARD -> Screen.bottomBarScreens
            UserMode.ELDERLY -> Screen.elderlyBottomBarScreens
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute !in listOf("splash", "onboarding", "privacy_consent")

    // 从数据库加载上次选中的成员（仅首次）
    val database = remember { AppDatabase.getDatabase(context.applicationContext) }
    LaunchedEffect(Unit) {
        com.yy.medtrace.ui.state.SelectedMemberHolder.initFromDatabase(database)
    }

    val activity = LocalContext.current as? ComponentActivity
    BackHandler {
        if (currentRoute == Screen.Home.route) {
            activity?.finish()
        } else if (!navController.popBackStack()) {
            navController.navigate(Screen.Home.route)
        }
    }

    LaunchedEffect(Unit) {
        if (initialRoute != null) {
            when (initialRoute) {
                "onboarding" -> navController.navigate("onboarding")
                "privacy_consent" -> navController.navigate("privacy_consent")
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    screens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                // 图标尺寸固定，避免选中态缩放造成的布局跳动
                                screen.icon(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    24.dp
                                )
                            },
                            label = {
                                Text(
                                    screen.label,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            // 胶囊高亮：ui.md 要求选中态有明显底色，而非仅靠图标缩放
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                if (screen.route == Screen.Home.route) {
                                    navController.navigate(Screen.Home.route) {
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enterTransition = { fadeIn(tween(150)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(150)) },
            popExitTransition = { fadeOut(tween(150)) }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    userModeStore = userModeStore
                )
            }
            composable("family") {
                val viewModel: FamilyViewModel = hiltViewModel()
                FamilyScreen(viewModel, navController)
            }
            composable(Screen.Reminders.route) {
                RemindersScreen(navController = navController, userModeStore = userModeStore)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(navController = navController)
            }
            composable(
                "add_record/{recordId}?memberId={memberId}&diagnosis={diagnosis}&hospital={hospital}&onsetTime={onsetTime}",
                arguments = listOf(
                    navArgument("recordId") { type = NavType.StringType },
                    navArgument("memberId") { type = NavType.StringType; defaultValue = "-1" },
                    navArgument("diagnosis") { type = NavType.StringType; defaultValue = "" },
                    navArgument("hospital") { type = NavType.StringType; defaultValue = "" },
                    navArgument("onsetTime") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("recordId")?.toLongOrNull() ?: -1L
                val memberId = backStackEntry.arguments?.getString("memberId")?.toLongOrNull() ?: -1L
                val diagnosis = backStackEntry.arguments?.getString("diagnosis") ?: ""
                val hospital = backStackEntry.arguments?.getString("hospital") ?: ""
                val onsetTime = backStackEntry.arguments?.getString("onsetTime") ?: ""
                val viewModel: AddMedicalRecordViewModel = hiltViewModel()
                AddMedicalRecordScreen(
                    viewModel, navController,
                    recordId = id,
                    memberId = memberId,
                    diagnosis = diagnosis,
                    hospital = hospital,
                    onsetTime = onsetTime
                )
            }
            composable("medical_records") {
                val viewModel: MedicalRecordViewModel = hiltViewModel()
                MedicalRecordScreen(viewModel, navController, userModeStore = userModeStore)
            }
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel, navController, userModeStore = userModeStore)
            }
            composable(
                "member_detail/{memberId}",
                arguments = listOf(navArgument("memberId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("memberId")?.toLongOrNull() ?: -1L
                val viewModel: MemberDetailViewModel = hiltViewModel()
                MemberDetailScreen(viewModel, navController, memberId = id)
            }
            composable("splash") {
                com.yy.medtrace.ui.screens.SplashScreen(navController)
            }
            composable("privacy_consent") {
                com.yy.medtrace.ui.screens.PrivacyConsentScreen(
                    navController = navController,
                    onDecline = { activity?.finish() }
                )
            }
            composable("onboarding") {
                com.yy.medtrace.ui.screens.OnboardingScreen(navController)
            }
            composable("privacy_policy") {
                com.yy.medtrace.ui.screens.PrivacyPolicyScreen(navController)
            }
            composable("user_agreement") {
                com.yy.medtrace.ui.screens.UserAgreementScreen(navController)
            }
        }
    }
}
