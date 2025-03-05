package com.yy.chiyaole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.WorkManager
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.ui.screens.*
import com.yy.chiyaole.ui.theme.ChiyaoleTheme

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var workManager: WorkManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        database = (application as ChiyaoleApplication).database
        workManager = WorkManager.getInstance(applicationContext)
        
        setContent {
            ChiyaoleTheme {
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

@Composable
fun MainScreen(database: AppDatabase, workManager: WorkManager) {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(database = database, navController = navController)
        }
        composable("medication_reminders") {
            MedicationReminderScreen(database, navController, workManager)
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
        composable("medical_records") {
            MedicalRecordScreen(database, navController)
        }
        composable("add_record") {
            AddMedicalRecordScreen(database, navController)
        }
    }
}