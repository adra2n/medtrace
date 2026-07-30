package com.yy.medtrace.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yy.medtrace.R
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.ui.components.EmptyState
import com.yy.medtrace.ui.components.MedicalRecordCard
import com.yy.medtrace.ui.components.MemberSelector
import com.yy.medtrace.ui.state.SelectedMemberHolder
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.viewmodel.MedicalRecordViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordScreen(
    viewModel: MedicalRecordViewModel = hiltViewModel(),
    navController: NavController
) {
    val database = viewModel.database
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
    var datePickerTarget by remember { mutableStateOf(DateTarget.From) }
    var showFilter by remember { mutableStateOf(false) }
    var showMemberMenu by remember { mutableStateOf(false) }
    val selectedMemberId = SelectedMemberHolder.selectedMemberId.value
    
    // 分页状态
    val pageSize = 20
    var currentPage by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
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
                    val fallbackId = latest?.patientId ?: list.first().id
                    scope.launch { SelectedMemberHolder.select(fallbackId, database) }
                }
            }
    }

    LaunchedEffect(selectedMemberId, keyword, fromDate, toDate) {
        selectedMemberId?.let { id ->
            val kw = keyword.trim().takeIf { it.isNotEmpty() }
            val from = fromDate?.atStartOfDay() ?: LocalDateTime.of(1970, 1, 1, 0, 0)
            val to = toDate?.atTime(23, 59, 59) ?: LocalDateTime.of(9999, 12, 31, 23, 59, 59)
            
            // 重置分页
            currentPage = 0
            hasMore = true
            records = emptyList()
            
            // 加载第一页
            try {
                val result = database.medicalRecordDao().searchByMemberPaged(
                    patientId = id,
                    keyword = kw,
                    likePattern = "%${kw ?: ""}%",
                    from = from,
                    to = to,
                    limit = pageSize,
                    offset = 0
                )
                records = result
                hasMore = result.size == pageSize
                currentPage = 1
            } catch (e: Exception) {
                error = e.message
                e.printStackTrace()
            }
        } ?: run { records = emptyList() }
    }
    
    // 加载更多
    fun loadMore() {
        if (isLoadingMore || !hasMore) return
        isLoadingMore = true
        
        scope.launch {
            try {
                val id = selectedMemberId ?: return@launch
                val kw = keyword.trim().takeIf { it.isNotEmpty() }
                val from = fromDate?.atStartOfDay() ?: LocalDateTime.of(1970, 1, 1, 0, 0)
                val to = toDate?.atTime(23, 59, 59) ?: LocalDateTime.of(9999, 12, 31, 23, 59, 59)
                
                val result = database.medicalRecordDao().searchByMemberPaged(
                    patientId = id,
                    keyword = kw,
                    likePattern = "%${kw ?: ""}%",
                    from = from,
                    to = to,
                    limit = pageSize,
                    offset = currentPage * pageSize
                )
                records = records + result
                hasMore = result.size == pageSize
                currentPage++
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoadingMore = false
            }
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = stringResource(R.string.medical_record_title),
                subtitle = if (records.isNotEmpty()) stringResource(R.string.medical_record_subtitle_count, records.size) else null,
                actions = {
                    Box {
                        IconButton(onClick = { showMemberMenu = true }) {
                            Icon(Icons.Default.People, stringResource(R.string.medical_record_cd_select_member), tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(
                            expanded = showMemberMenu,
                            onDismissRequest = { showMemberMenu = false }
                        ) {
                            members.forEach { member ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (member.relation.isNotBlank()) "${member.name}（${member.relation}）" else member.name,
                                            color = if (member.id == selectedMemberId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        scope.launch { SelectedMemberHolder.select(member.id, database) }
                                        showMemberMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { navController.navigate("add_record") }) {
                        Icon(Icons.Default.Add, stringResource(R.string.medical_record_cd_add), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    ) { padding ->
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
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
                                    text = stringResource(R.string.medical_record_overview),
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
                                    label = stringResource(R.string.medical_record_stat_total),
                                    modifier = Modifier.weight(1f)
                                )
                                StatItem(
                                    value = records.count {
                                        it.onsetTime.month == java.time.Month.from(java.time.LocalDate.now())
                                    }.toString(),
                                    label = stringResource(R.string.medical_record_stat_month),
                                    modifier = Modifier.weight(1f)
                                )
                                StatItem(
                                    value = records.sumOf { it.medItems.size }.toString(),
                                    label = stringResource(R.string.medical_record_stat_medications),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
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
                                text = stringResource(R.string.medical_record_search_filter),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.medical_record_search_placeholder)) },
                            singleLine = true,
                            trailingIcon = {
                                Row {
                                    if (keyword.isNotEmpty()) {
                                        IconButton(onClick = { keyword = "" }) {
                                            Icon(Icons.Default.Close, stringResource(R.string.medical_record_cd_clear))
                                        }
                                    }
                                    IconButton(onClick = { showFilter = !showFilter }) {
                                        Icon(
                                            Icons.Default.Settings,
                                            stringResource(R.string.medical_record_cd_filter),
                                            tint = if (showFilter || fromDate != null || toDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Search, stringResource(R.string.medical_record_cd_search)) }
                        )
                        if (showFilter) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterDateChip(
                                    label = stringResource(R.string.medical_record_filter_from),
                                    value = fromDate?.format(dayFormatter),
                                    onClick = {
                                        datePickerTarget = DateTarget.From
                                        showDatePicker = true
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterDateChip(
                                    label = stringResource(R.string.medical_record_filter_to),
                                    value = toDate?.format(dayFormatter),
                                    onClick = {
                                        datePickerTarget = DateTarget.To
                                        showDatePicker = true
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
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
                                Text(stringResource(R.string.medical_record_btn_reset))
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
                            text = stringResource(R.string.medical_record_list_title),
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
                        Text(stringResource(R.string.medical_record_error_loading, error ?: ""), color = MaterialTheme.colorScheme.error)
                    }
                }
            } else if (records.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.MedicalServices,
                        title = if (keyword.isNotEmpty() || fromDate != null || toDate != null) {
                            stringResource(R.string.medical_record_empty_no_match)
                        } else {
                            stringResource(R.string.medical_record_empty_no_records)
                        },
                        hint = if (keyword.isEmpty() && fromDate == null && toDate == null) {
                            stringResource(R.string.medical_record_empty_hint)
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

        val shouldLoadMore by remember {
            derivedStateOf {
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalItems = listState.layoutInfo.totalItemsCount
                lastVisibleItem >= totalItems - 3 && !isLoadingMore && hasMore
            }
        }

        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore) {
                loadMore()
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
            title = { Text(stringResource(R.string.medical_record_dialog_delete_title)) },
            text = { Text(stringResource(R.string.medical_record_dialog_delete_message, record.diagnosis)) },
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
                }) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.btn_cancel)) }
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
