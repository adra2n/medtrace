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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleEventObserver
import com.yy.medtrace.data.security.BiometricHelper
import com.yy.medtrace.data.security.PinManager
import com.yy.medtrace.data.settings.SecuritySettingsStore
import com.yy.medtrace.ui.screens.LockScreen
import com.yy.medtrace.ui.screens.MemberDetailScreen
import com.yy.medtrace.ui.theme.Primary
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.UserSettings
import com.yy.medtrace.ui.screens.AddMedicalRecordScreen
import com.yy.medtrace.ui.screens.FamilyScreen
import com.yy.medtrace.ui.screens.HomeScreen
import com.yy.medtrace.ui.screens.MedicalRecordScreen
import com.yy.medtrace.ui.screens.SettingsScreen
import com.yy.medtrace.ui.screens.SplashScreen
import com.yy.medtrace.ui.screens.OnboardingScreen
import com.yy.medtrace.ui.screens.PrivacyConsentScreen
import com.yy.medtrace.ui.screens.TrendsScreen
import com.yy.medtrace.ui.theme.ChiyaoleTheme
import com.yy.medtrace.reminder.ReminderHelper
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
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
                    FamilyMember(name = "我自己", relation = "本人", isDefault = true)
                )
            }
        }

        setContent {
            val settings by database.userSettingsDao().getUserSettings().collectAsState(initial = null)
            ChiyaoleTheme(darkTheme = settings?.darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(database)
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

sealed class Screen(
    val route: String,
    val label: String,
    val icon: @Composable (tint: androidx.compose.ui.graphics.Color, size: androidx.compose.ui.unit.Dp) -> Unit
) {
    object Home : Screen("home", "首页", { tint, size ->
        Icon(Icons.Filled.Home, "首页", tint = tint, modifier = Modifier.size(size))
    })
    object Family : Screen("family", "家人档案", { tint, size ->
        Icon(Icons.Filled.People, "家人档案", tint = tint, modifier = Modifier.size(size))
    })
    object AddRecord : Screen("add_record", "新增记录", { tint, size ->
        Icon(Icons.Filled.AddCircle, "新增记录", tint = tint, modifier = Modifier.size(size))
    })
    object Health : Screen("trends", "健康分析", { tint, size ->
        Icon(Icons.Filled.MonitorHeart, "健康分析", tint = tint, modifier = Modifier.size(size))
    })
    object Settings : Screen("settings", "设置", { tint, size ->
        Icon(Icons.Filled.Settings, "设置", tint = tint, modifier = Modifier.size(size))
    })
}

@Composable
fun SettingsAction(navController: NavController) {
    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
        Icon(Icons.Default.Settings, "设置")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(database: AppDatabase) {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Home,
        Screen.Family
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in screens.map { it.route } && currentRoute != null

    val topLevelRoutes = screens.map { it.route }
    val activity = LocalContext.current as? ComponentActivity
    BackHandler(enabled = currentRoute in topLevelRoutes) {
        if (currentRoute == Screen.Home.route) {
            activity?.finish()
        } else {
            navController.popBackStack(Screen.Home.route, false)
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            screens.forEach { screen ->
                                val selected = currentRoute == screen.route
                                val contentColor = if (selected) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (selected) Primary.copy(alpha = 0.12f) else Color.Transparent
                                        )
                                        .clickable {
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Home.route) {
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .weight(1f)
                                ) {
                                    screen.icon(contentColor, if (selected) 26.dp else 22.dp)
                                }
                            }
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(padding)
        ) {
            composable("splash") {
                SplashScreen(navController)
            }
            composable("privacy_consent") {
                PrivacyConsentScreen(
                    navController = navController,
                    onDecline = { activity?.finish() }
                )
            }
            composable("onboarding") {
                OnboardingScreen(navController)
            }
            composable(Screen.Home.route) {
                HomeScreen(database, navController)
            }
            composable(Screen.Family.route) {
                FamilyScreen(database, navController)
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
            composable("add_record/{recordId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("recordId")?.toLongOrNull() ?: -1L
                AddMedicalRecordScreen(database, navController, recordId = id)
            }
            composable("member_detail/{memberId}?tab={tab}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("memberId")?.toLongOrNull() ?: -1L
                val tab = backStackEntry.arguments?.getString("tab")
                MemberDetailScreen(database, navController, memberId = id, initialTab = tab)
            }
        }
    }
}
