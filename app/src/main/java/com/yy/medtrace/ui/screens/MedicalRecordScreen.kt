package com.yy.medtrace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.ui.components.EmptyState
import com.yy.medtrace.ui.components.MemberSelector
import com.yy.medtrace.ui.state.SelectedMemberHolder
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
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
    val selectedMemberId = SelectedMemberHolder.selectedMemberId.value
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    LaunchedEffect(Unit) {
        database.familyMemberDao().getAllMembers()
            .catch { e -> error = e.message }
            .collect { list ->
                if (list.isEmpty()) {
                    scope.launch {
                        database.familyMemberDao().insert(
                            FamilyMember.DEFAULT
                        )
                    }
                    return@collect
                }
                members = list + com.yy.medtrace.ui.state.UNKNOWN_MEMBER
                if (SelectedMemberHolder.selectedMemberId.value == null) {
                    val latest = database.medicalRecordDao().getLatestRecord()
                    SelectedMemberHolder.selectedMemberId.value =
                        latest?.patientId ?: list.first().id
                }
            }
    }

    LaunchedEffect(selectedMemberId, keyword, fromDate, toDate) {
        selectedMemberId?.let { id ->
            val kw = keyword.trim().takeIf { it.isNotEmpty() }
            val from = fromDate?.atStartOfDay() ?: LocalDateTime.of(1970, 1, 1, 0, 0)
            val to = toDate?.atTime(23, 59, 59) ?: LocalDateTime.of(9999, 12, 31, 23, 59, 59)
            val flow = if (id == 0L) {
                database.medicalRecordDao().getUnknownRecords()
            } else {
                database.medicalRecordDao()
                    .searchByMember(id, kw, "%${kw ?: ""}%", from, to)
            }
            flow
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
            GradientTopBar(
                title = "就诊记录",
                subtitle = if (records.isNotEmpty()) "共 ${records.size} 条记录" else null,
                actions = {
                    IconButton(onClick = { navController.navigate("add_record") }) {
                        Icon(Icons.Default.Add, "添加记录", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 统计概览卡片
            if (records.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.large,
                        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "记录概览",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem(
                                    value = records.size.toString(),
                                    label = "总记录",
                                    modifier = Modifier.weight(1f)
                                )
                                StatItem(
                                    value = records.count {
                                        it.onsetTime.month == java.time.Month.from(java.time.LocalDate.now())
                                    }.toString(),
                                    label = "本月",
                                    modifier = Modifier.weight(1f)
                                )
                                StatItem(
                                    value = records.sumOf { it.medItems.size }.toString(),
                                    label = "药品",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 成员选择
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.large,
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                    elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "选择成员",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        MemberSelector(
                            members = members,
                            selectedMemberId = selectedMemberId,
                            onSelect = { member -> scope.launch { SelectedMemberHolder.select(member.id, database) } },
                            emptyHint = "暂无家庭成员，请先在家庭中添加"
                        )
                    }
                }
            }

            // 搜索筛选
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.large,
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                    elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "搜索筛选",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("搜索就诊类型 / 医院 / 备注") },
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
                        }
                        if (keyword.isNotEmpty() || fromDate != null || toDate != null) {
                            TextButton(
                                onClick = {
                                    keyword = ""
                                    fromDate = null
                                    toDate = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("重置筛选")
                            }
                        }
                    }
                }
            }

            // 记录列表标题
            if (records.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "记录列表",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 记录列表
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
                    EmptyState(
                        icon = Icons.Default.MedicalServices,
                        title = if (keyword.isNotEmpty() || fromDate != null || toDate != null) {
                            "没有符合筛选条件的记录"
                        } else {
                            "该成员还没有就诊记录"
                        },
                        hint = if (keyword.isEmpty() && fromDate == null && toDate == null) {
                            "点击右上角 + 按钮添加第一条记录"
                        } else null,
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            } else {
                items(records) { record ->
                    MedicalRecordCard(
                        record = record,
                        dateFormatter = dateFormatter,
                        onEdit = { navController.navigate("add_record/${record.id}") },
                        onDelete = { pendingDelete = record }
                    )
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
            title = { Text("删除就诊记录") },
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

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MedicalRecordCard(
    record: MedicalRecord,
    dateFormatter: DateTimeFormatter,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 卡片头部：成员名称和时间（同一行）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = record.patientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = record.onsetTime.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 卡片内容
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 诊断信息
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = record.diagnosis,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 就诊医院
                if (record.hospital.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = record.hospital,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 开具药品
                if (record.medItems.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "开具药品",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        record.medItems.forEach { med ->
                            val parts = listOf(med.name, med.dose, med.freq, med.duration)
                                .filter { it.isNotBlank() }
                                .joinToString(" ")
                            Row(
                                modifier = Modifier.padding(start = 26.dp)
                            ) {
                                Text(
                                    text = "·",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = parts,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 备注
                if (record.notes.isNotBlank()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = record.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 操作按钮行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Edit, "编辑", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("编辑")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, "删除", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("删除")
                }
            }
        }
    }
}

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
