package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.ui.state.SelectedMemberHolder
import com.yy.chiyaole.ui.theme.cardContainerColor
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
                title = { Text("医疗记录") }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        if (selectedMemberId != null) {
                            val current = members.firstOrNull { it.id == selectedMemberId }
                            if (current != null) {
                                Text(
                                    text = "当前：${current.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (members.isEmpty()) {
                        Text(
                            text = "暂无家庭成员，请先在设置中添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            members.forEach { member ->
                                val selected = member.id == selectedMemberId
                                Card(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .clickable { SelectedMemberHolder.recordsSelectedMemberId.value = member.id },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    border = if (selected)
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    else
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (member.isDefault) Icons.Filled.Person else Icons.Filled.People,
                                            contentDescription = null,
                                            tint = if (selected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (selected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                        if (member.relation.isNotBlank()) {
                                            Text(
                                                text = member.relation,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (selected)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
