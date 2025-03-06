package com.yy.chiyaole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.WorkManager
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.ui.screens.*
import com.yy.chiyaole.ui.theme.ChiyaoleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
    object Settings : Screen(
        route = "settings",
        icon = { Icon(Icons.Default.Settings, "设置") },
        label = "设置"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(database: AppDatabase, workManager: WorkManager) {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Home,
        Screen.MedicationReminders,
        Screen.MedicalRecords,
        Screen.Settings
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
                MedicationReminderScreen(database, navController, workManager)
            }
            composable(Screen.MedicalRecords.route) {
                MedicalRecordScreen(database, navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable("add_reminder") {
                AddMedicationReminderScreen(database, navController, workManager, reminderId = null)
            }
            composable(
                route = "edit_reminder/{reminderId}",
                arguments = listOf(
                    navArgument("reminderId") { type = NavType.LongType }
                )
            ) {
                val reminderId = it.arguments?.getLong("reminderId")
                AddMedicationReminderScreen(database, navController, workManager, reminderId)
            }
            composable("add_record") {
                AddMedicalRecordScreen(database, navController)
            }
        }
    }
}