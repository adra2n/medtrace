package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.ui.components.MemberSelector
import com.yy.chiyaole.SettingsAction
import com.yy.chiyaole.ui.state.SelectedMemberHolder
import com.yy.chiyaole.ui.theme.AppShapes
import com.yy.chiyaole.ui.theme.cardContainerColor
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordScreen(
    database: AppDatabase,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var records by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<MedicalRecord?>(null) }
    var keyword by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf<LocalDate?>(null) }
    var toDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf<DateTarget>(DateTarget.From) }
    val selectedMemberId = SelectedMemberHolder.recordsSelectedMemberId.value
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

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

    LaunchedEffect(selectedMemberId, keyword, fromDate, toDate) {
        selectedMemberId?.let { id ->
            val kw = keyword.trim().takeIf { it.isNotEmpty() }
            val from = fromDate?.atStartOfDay() ?: LocalDateTime.of(1970, 1, 1, 0, 0)
            val to = toDate?.atTime(23, 59, 59) ?: LocalDateTime.of(9999, 12, 31, 23, 59, 59)
            database.medicalRecordDao()
                .searchByMember(id, kw, "%${kw ?: ""}%", from, to)
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

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索诊断 / 医院 / 备注") },
                        singleLine = true,
                        trailingIcon = {
                            if (keyword.isNotEmpty()) {
                                IconButton(onClick = { keyword = "" }) {
                                    Icon(Icons.Default.Close, "清除")
                                }
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Search, "搜索") }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterDateChip(
                            label = "起始",
                            value = fromDate?.format(dayFormatter),
                            onClick = {
                                datePickerTarget = DateTarget.From
                                showDatePicker = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterDateChip(
                            label = "结束",
                            value = toDate?.format(dayFormatter),
                            onClick = {
                                datePickerTarget = DateTarget.To
                                showDatePicker = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (keyword.isNotEmpty() || fromDate != null || toDate != null) {
                            TextButton(onClick = {
                                keyword = ""
                                fromDate = null
                                toDate = null
                            }) { Text("重置") }
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
                            text = if (keyword.isNotEmpty() || fromDate != null || toDate != null) {
                                "没有符合筛选条件的记录"
                            } else {
                                "该成员还没有医疗记录"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(records) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.medium,
                        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                        onClick = { pendingDelete = record }
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

    if (showDatePicker) {
        val initial = (if (datePickerTarget == DateTarget.From) fromDate else toDate)
            ?: LocalDate.now()
        val dialog = android.app.DatePickerDialog(
            context,
            { _, y, m, d ->
                val picked = LocalDate.of(y, m + 1, d)
                if (datePickerTarget == DateTarget.From) fromDate = picked else toDate = picked
                showDatePicker = false
            },
            initial.year, initial.monthValue - 1, initial.dayOfMonth
        )
        dialog.setOnCancelListener { showDatePicker = false }
        dialog.show()
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除医疗记录") },
            text = { Text("确定删除「${record.diagnosis}」这条记录？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            database.medicalRecordDao().delete(record)
                        } catch (e: Exception) {
                            error = e.message
                            e.printStackTrace()
                        }
                    }
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

private enum class DateTarget { From, To }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDateChip(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(if (value != null) "$label：$value" else label)
        },
        modifier = modifier
    )
}
