package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.ui.components.MemberSelector
import com.yy.chiyaole.SettingsAction
import com.yy.chiyaole.ui.state.SelectedMemberHolder
import com.yy.chiyaole.ui.theme.cardContainerColor
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordScreen(
    database: AppDatabase,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var records by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val selectedMemberId = SelectedMemberHolder.recordsSelectedMemberId.value
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

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
                if (SelectedMemberHolder.recordsSelectedMemberId.value == null) {
                    val latest = database.medicalRecordDao().getLatestRecord()
                    SelectedMemberHolder.recordsSelectedMemberId.value =
                        latest?.patientId ?: list.first().id
                }
            }
    }

    LaunchedEffect(selectedMemberId) {
        selectedMemberId?.let { id ->
            database.medicalRecordDao().getRecordsByMember(id)
                .catch { e ->
                    error = e.message
                    e.printStackTrace()
                }
                .collectLatest {
                    records = it
                }
        } ?: run { records = emptyList() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("医疗记录") },
                actions = { SettingsAction(navController) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_record") }
            ) {
                Icon(Icons.Default.Add, "添加记录")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "家庭成员",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        members.firstOrNull { it.id == selectedMemberId }?.let { current ->
                            Text(
                                text = "当前：${current.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    MemberSelector(
                        members = members,
                        selectedMemberId = selectedMemberId,
                        onSelect = { SelectedMemberHolder.recordsSelectedMemberId.value = it.id },
                        emptyHint = "暂无家庭成员，请先在家庭中添加"
                    )
                }
            }

            if (error != null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("加载数据时出错：$error", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else if (records.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "该成员还没有医疗记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(records) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = record.patientName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row {
                                    IconButton(
                                        onClick = { navController.navigate("add_record/${record.id}") }
                                    ) {
                                        Icon(Icons.Default.Edit, "编辑")
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    database.medicalRecordDao().delete(record)
                                                } catch (e: Exception) {
                                                    error = e.message
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, "删除")
                                    }
                                }
                            }

                            Text(
                                text = "诊断结果：${record.diagnosis}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                text = "就诊时间：${record.onsetTime.format(dateFormatter)}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (record.hospital.isNotBlank()) {
                                Text(
                                    text = "就诊医院：${record.hospital}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (record.medItems.isNotEmpty()) {
                                Text(
                                    text = "开具药品：",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                record.medItems.forEach { med ->
                                    val parts = listOf(med.name, med.dose, med.freq, med.duration)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" ")
                                    Text(
                                        text = "· $parts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            if (record.notes.isNotBlank()) {
                                Text(
                                    text = "备注：${record.notes}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
