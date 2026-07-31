package com.yy.medtrace

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.app.AlarmManager
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import androidx.activity.compose.BackHandler
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.UserSettings
import com.yy.medtrace.data.security.BiometricHelper
import com.yy.medtrace.data.security.PinManager
import com.yy.medtrace.data.settings.PremiumManager
import com.yy.medtrace.data.settings.SecuritySettingsStore
import com.yy.medtrace.navigation.Screen
import com.yy.medtrace.ui.screens.AddMedicalRecordScreen
import com.yy.medtrace.ui.screens.FamilyScreen
import com.yy.medtrace.ui.screens.HomeScreen
import com.yy.medtrace.ui.screens.LockScreen
import com.yy.medtrace.ui.screens.MedicalRecordScreen
import com.yy.medtrace.ui.screens.MemberDetailScreen
import com.yy.medtrace.ui.screens.SettingsScreen
import com.yy.medtrace.ui.screens.RemindersScreen
import com.yy.medtrace.ui.screens.ProfileScreen
import com.yy.medtrace.ui.screens.TrendsScreen
import com.yy.medtrace.ui.screens.PremiumScreen
import com.yy.medtrace.ui.theme.Background
import com.yy.medtrace.ui.theme.ChiyaoleTheme
import com.yy.medtrace.ui.theme.Primary
import com.yy.medtrace.reminder.ReminderHelper
import com.yy.medtrace.viewmodel.AddMedicalRecordViewModel
import com.yy.medtrace.viewmodel.FamilyViewModel
import com.yy.medtrace.viewmodel.MedicalRecordViewModel
import com.yy.medtrace.viewmodel.MemberDetailViewModel
import com.yy.medtrace.viewmodel.TrendsViewModel
import com.yy.medtrace.viewmodel.SettingsViewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var premiumManager: PremiumManager
    
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)

        // 健康待办每日提醒：申请通知权限（Android 13+）后排程定时提醒
        requestReminderPermissionAndSchedule()

        // 历史版本（v1–v5）因迁移缺失导致旧库被重置：提示用户从备份恢复。
        if (AppDatabase.migrationResetHappened) {
            Toast.makeText(
                this,
                "数据库已因版本升级重建，旧数据已清空。请到「设置 → 数据备份与恢复」从备份恢复。",
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
                        premiumManager = premiumManager,
                        initialRoute = navigateTo
                    )
                }
            }
        }
    }

    private fun requestReminderPermissionAndSchedule() {
        val requestLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) ReminderHelper.scheduleDaily(this)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    scheduleIfExactAlarmAllowed()
                }
                else -> requestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            ReminderHelper.scheduleDaily(this)
        }
    }

    private val exactAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 从精确闹钟设置页返回后重试一次：若用户已授权则排程，未授权则静默放弃
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (getSystemService(ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        ) {
            ReminderHelper.scheduleDaily(this)
        }
    }

    private fun scheduleIfExactAlarmAllowed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showExactAlarmHintDialog()
                return
            }
        }
        ReminderHelper.scheduleDaily(this)
    }

    private fun showExactAlarmHintDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("开启每日健康提醒")
            .setMessage(
                "为了每天 9 点准时弹出「今日健康待办」提醒，请允许医迹使用" +
                    "「精确闹钟」权限。\n\n不开启则每日提醒不会触发，其他功能不受影响。"
            )
            .setNegativeButton("暂不") { _, _ -> }
            .setPositiveButton("去设置") { _, _ ->
                exactAlarmLauncher.launch(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                )
            }
            .show()
    }
}

// Screen类已移动到navigation包

@Composable
fun SettingsAction(navController: NavController) {
    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
        Icon(Icons.Default.Settings, "设置")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    premiumManager: com.yy.medtrace.data.settings.PremiumManager,
    initialRoute: String? = null
) {
    val navController = rememberNavController()
    val screens = Screen.bottomBarScreens

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in screens.map { it.route }

    // 从数据库加载上次选中的成员（仅首次）
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context.applicationContext) }
    LaunchedEffect(Unit) {
        com.yy.medtrace.ui.state.SelectedMemberHolder.initFromDatabase(database)
    }

    val activity = LocalContext.current as? ComponentActivity
    BackHandler(enabled = showBottomBar) {
        if (currentRoute == Screen.Home.route) {
            activity?.finish()
        } else {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        }
    }

    val fragmentActivity = activity as? androidx.fragment.app.FragmentActivity
    var appLockEnabled by remember { mutableStateOf(false) }
    var autoLockSeconds by remember { mutableStateOf(0) }
    var secureScreen by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }

    val secStore = remember(fragmentActivity) {
        fragmentActivity?.let { SecuritySettingsStore(it) }
    }
    val pinSet = remember(fragmentActivity) {
        fragmentActivity?.let { PinManager.isPinSet(it) } ?: false
    }
    val biometricAvailable = remember(fragmentActivity) {
        fragmentActivity?.let { BiometricHelper.canAuthenticate(it) } ?: false
    }

    LaunchedEffect(Unit) {
        appLockEnabled = secStore?.getAppLockEnabled() ?: false
        autoLockSeconds = secStore?.getAutoLockSeconds() ?: 0
        secureScreen = secStore?.getSecureScreen() ?: false
        activity?.window?.setFlags(
            if (secureScreen) android.view.WindowManager.LayoutParams.FLAG_SECURE else 0,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        locked = appLockEnabled

        if (initialRoute != null) {
            when (initialRoute) {
                "onboarding" -> navController.navigate("onboarding")
                "privacy_consent" -> navController.navigate("privacy_consent")
            }
        }
    }

    var backgroundedAt by remember { mutableStateOf(0L) }
    DisposableEffect(fragmentActivity) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    if (appLockEnabled) {
                        backgroundedAt = android.os.SystemClock.elapsedRealtime()
                        locked = true
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    if (appLockEnabled && backgroundedAt > 0) {
                        val elapsed = (android.os.SystemClock.elapsedRealtime() - backgroundedAt) / 1000
                        if (elapsed >= autoLockSeconds) locked = true
                        backgroundedAt = 0
                    }
                }
                else -> {}
            }
        }
        fragmentActivity?.lifecycle?.addObserver(observer)
        onDispose { fragmentActivity?.lifecycle?.removeObserver(observer) }
    }

    fun promptBiometric() {
        fragmentActivity?.let {
            BiometricHelper.authenticate(
                activity = it,
                onSuccess = { locked = false },
                onError = { /* 保持锁定 */ }
            )
        } ?: run { locked = false }
    }

    if (locked) {
        LockScreen(
            pinEnabled = pinSet,
            biometricEnabled = biometricAvailable,
            onBiometricClick = { promptBiometric() },
            onPinEntered = { pin ->
                val ok = fragmentActivity?.let { PinManager.verify(it, pin) } ?: false
                if (ok) locked = false
                ok
            },
            onForgotPin = {
                activity?.let { act ->
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", act.packageName, null)
                    )
                    act.startActivity(intent)
                }
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                screen.icon(
                                    if (currentRoute == screen.route) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    if (currentRoute == screen.route) 24.dp else 22.dp
                                )
                            },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
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
                .padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController
                )
            }
            composable("family") {
                val viewModel: FamilyViewModel = hiltViewModel()
                FamilyScreen(viewModel, navController)
            }
            composable(Screen.Reminders.route) {
                RemindersScreen(navController = navController)
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
            composable("premium") {
                PremiumScreen(
                    navController = navController,
                    premiumManager = premiumManager
                )
            }
        }
    }
}
