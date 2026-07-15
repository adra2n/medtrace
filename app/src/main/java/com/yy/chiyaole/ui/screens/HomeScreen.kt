package com.yy.chiyaole.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.ui.components.EmptyRecords
import com.yy.chiyaole.ui.components.HealthTipsCard
import com.yy.chiyaole.ui.components.MedicalRecordCard
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    database: AppDatabase,
    navController: NavController
) {
    var recentRecords by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        database.medicalRecordDao().getRecentRecords(1)
            .catch { e ->
                error = e.message
                e.printStackTrace()
            }
            .collect { records ->
                recentRecords = records
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智药乐") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Text(
                    text = "最新医疗记录",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (recentRecords.isEmpty()) {
                item {
                    EmptyRecords()
                }
            } else {
                items(recentRecords) { record ->
                    MedicalRecordCard(record)
                }
            }

            item {
                HealthTipsCard()
            }
        }
    }
}
