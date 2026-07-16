package com.yy.chiyaole.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.ui.components.EmptyRecords
import com.yy.chiyaole.ui.state.SelectedMemberHolder
import com.yy.chiyaole.ui.components.HealthTipsCard
import com.yy.chiyaole.ui.components.MedicalRecordCard
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    database: AppDatabase,
    navController: NavController
) {
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var recentRecords by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val selectedMemberId = SelectedMemberHolder.homeSelectedMemberId.value

    LaunchedEffect(Unit) {
        database.familyMemberDao().getAllMembers()
            .catch { e -> error = e.message }
            .collect { list ->
                if (list.isEmpty()) {
                    scope.launch {
                        database.familyMemberDao().insert(
                            FamilyMember(name = "我自己", relation = "本人", isDefault = true)
                        )
                    }
                    return@collect
                }
                members = list
                if (SelectedMemberHolder.homeSelectedMemberId.value == null) {
                    val latest = database.medicalRecordDao().getLatestRecord()
                    SelectedMemberHolder.homeSelectedMemberId.value =
                        latest?.patientId ?: list.first().id
                }
            }
    }

    LaunchedEffect(selectedMemberId) {
        selectedMemberId?.let { id ->
            database.medicalRecordDao().getRecentRecordsByMember(id, 1)
                .catch { e -> error = e.message }
                .collect { records -> recentRecords = records }
        } ?: run { recentRecords = emptyList() }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    members.forEach { member ->
                        FilterChip(
                            selected = member.id == selectedMemberId,
                            onClick = { SelectedMemberHolder.homeSelectedMemberId.value = member.id },
                            label = { Text(if (member.relation.isNotBlank()) "${member.name}（${member.relation}）" else member.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (member.isDefault) Icons.Filled.Person else Icons.Filled.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

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
