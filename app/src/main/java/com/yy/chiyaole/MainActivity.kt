package com.yy.chiyaole

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.UserSettings
import com.yy.chiyaole.ui.screens.AboutScreen
import com.yy.chiyaole.ui.screens.AddMedicalRecordScreen
import com.yy.chiyaole.ui.screens.SettingsScreen
import com.yy.chiyaole.ui.screens.HomeScreen
import com.yy.chiyaole.ui.screens.MedicalRecordScreen
import com.yy.chiyaole.ui.screens.SplashScreen
import com.yy.chiyaole.ui.theme.ChiyaoleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)

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
}

sealed class Screen(val route: String, val icon: @Composable () -> Unit, val label: String) {
    object Home : Screen(
        route = "home",
        icon = { Icon(Icons.Default.Home, "首页") },
        label = "首页"
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
    object Settings : Screen(
        route = "settings",
        icon = { Icon(Icons.Default.Settings, "设置") },
        label = "设置"
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(database: AppDatabase) {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Home,
        Screen.MedicalRecords,
        Screen.Settings,
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
            composable(Screen.MedicalRecords.route) {
                MedicalRecordScreen(database, navController)
            }
            composable(Screen.About.route) {
                AboutScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(database)
            }
            composable("add_record") {
                AddMedicalRecordScreen(database, navController)
            }
            composable("add_record/{recordId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("recordId")?.toLongOrNull() ?: -1L
                AddMedicalRecordScreen(database, navController, recordId = id)
            }
        }
    }
}
