package com.yy.chiyaole

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.work.WorkManager
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.ui.screens.AboutScreen
import com.yy.chiyaole.ui.screens.AddMedicalRecordScreen
import com.yy.chiyaole.ui.screens.AddMedicationReminderScreen
import com.yy.chiyaole.ui.screens.HomeScreen
import com.yy.chiyaole.ui.screens.MedicalRecordScreen
import com.yy.chiyaole.ui.screens.MedicationReminderScreen
import com.yy.chiyaole.ui.screens.SplashScreen
import com.yy.chiyaole.ui.theme.ChiyaoleTheme
import com.yy.chiyaole.util.NotificationUtil

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 用户授予了通知权限
            NotificationUtil.createNotificationChannel(this)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 检查并请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 已有权限，创建通知渠道
                    NotificationUtil.createNotificationChannel(this)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // 显示权限解释对话框，然后请求权限
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // 直接请求权限
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 13 以下版本，直接创建通知渠道
            NotificationUtil.createNotificationChannel(this)
        }
        
        val database = AppDatabase.getDatabase(applicationContext)
        val workManager = WorkManager.getInstance(applicationContext)
        
        setContent {
            val settings by database.userSettingsDao().getUserSettings().collectAsState(initial = null)
            ChiyaoleTheme(darkTheme = settings?.darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(database, workManager)
                }
            }
        }
    }
}

sealed class Screen(val route: String, val icon: @Composable () -> Unit, val label: String) {
    object Home : Screen(
        route = "home",
        icon = { Icon(Icons.Default.Home, "首页") },
        label = "首页"
    )
    object MedicationReminders : Screen(
        route = "medication_reminders",
        icon = { Icon(Icons.Default.DateRange, "用药提醒") },
        label = "用药提醒"
    )
    object MedicalRecords : Screen(
        route = "medical_records",
        icon = { Icon(Icons.Default.Person, "医疗记录") },
        label = "医疗记录"
    )
    object About : Screen(
        route = "about",
        icon = { Icon(Icons.Filled.Info, "关于") },
        label = "关于"
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(database: AppDatabase, workManager: WorkManager) {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Home,
        Screen.MedicationReminders,
        Screen.MedicalRecords,
        Screen.About
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != "splash" && currentRoute != null

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = screen.icon,
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
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
            startDestination = "splash",
            modifier = Modifier.padding(padding)
        ) {
            composable("splash") {
                SplashScreen(navController)
            }
            composable(Screen.Home.route) {
                HomeScreen(database, navController)
            }

             composable(Screen.MedicationReminders.route) {
                 MedicationReminderScreen(database, navController)
             }
             composable(Screen.MedicalRecords.route) {
                 MedicalRecordScreen(database, navController)
             }
             composable(Screen.About.route) {
                 AboutScreen()
             }

             composable("add_reminder") {
                 AddMedicationReminderScreen(database, navController, reminderId = null)
             }

             composable(
                 route = "edit_reminder/{reminderId}",
                 arguments = listOf(
                     navArgument("reminderId") { type = NavType.LongType }
                 )
             ) {
                 val reminderId = it.arguments?.getLong("reminderId")
                 AddMedicationReminderScreen(database, navController, reminderId)
             }
             composable("add_record") {
                 AddMedicalRecordScreen(database, navController)
             }
        }
    }
}