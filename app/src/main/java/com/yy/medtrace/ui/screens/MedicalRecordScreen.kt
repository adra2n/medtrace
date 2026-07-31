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
import com.yy.medtrace.ui.state.SelectedMemberHolder
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.viewmodel.MedicalRecordViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordScreen(
    viewModel: MedicalRecordViewModel = hiltViewModel(),
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var pendingDelete by remember { mutableStateOf<MedicalRecord?>(null) }
    var keyword by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf<LocalDate?>(null) }
    var toDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf(DateTarget.From) }
    var showFilter by remember { mutableStateOf(false) }
    var showMemberMenu by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf("time") }
    val selectedMemberId = SelectedMemberHolder.selectedMemberId.value
    
    // 分页状态
    val pageSize = 20
    var currentPage by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    LaunchedEffect(Unit) {
        viewModel.loadMembers()
        if (SelectedMemberHolder.selectedMemberId.value == null) {
            val latest = viewModel.getLatestRecord()
            val defaultMember = viewModel.getDefaultMember()
            val fallbackId = latest?.patientId ?: defaultMember?.id ?: 1L
            scope.launch { SelectedMemberHolder.select(fallbackId, viewModel.database) }
        }
    }

    LaunchedEffect(selectedMemberId, keyword, fromDate, toDate) {
        selectedMemberId?.let { id ->
            val kw = keyword.trim().takeIf { it.isNotEmpty() }
            val from = fromDate?.atStartOfDay()?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            val to = toDate?.atTime(23, 59, 59)?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            
            // 重置分页
            currentPage = 0
            hasMore = true
            
            // 加载第一页
            viewModel.loadRecords(id, kw, from, to)
            currentPage = 1
        } ?: run { viewModel.loadRecords(0) }
    }
    
    // 加载更多
    fun loadMore() {
        if (isLoadingMore || !hasMore) return
        isLoadingMore = true
        
        scope.launch {
            try {
                val id = selectedMemberId ?: return@launch
                val kw = keyword.trim().takeIf { it.isNotEmpty() }
                val from = fromDate?.atStartOfDay()?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                val to = toDate?.atTime(23, 59, 59)?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                
                // 由于 ViewModel 的 loadRecords 方法会覆盖之前的数据，这里需要保留旧数据
                val oldRecords = uiState.records
                viewModel.loadRecords(id, kw, from, to)
                // 这里简化处理，实际应该支持增量加载
                hasMore = false
                currentPage++
            } catch (e: Exception) {
                viewModel.clearError()
            } finally {
                isLoadingMore = false
            }
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = stringResource(R.string.medical_record_title),
                subtitle = if (uiState.records.isNotEmpty()) stringResource(R.string.medical_record_subtitle_count, uiState.records.size) else null,
                actions = {
                    Box {
                        IconButton(onClick = { showMemberMenu = true }) {
                            Icon(Icons.Default.People, stringResource(R.string.medical_record_cd_select_member), tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(
                            expanded = showMemberMenu,
                            onDismissRequest = { showMemberMenu = false }
                        ) {
                            uiState.members.forEach { member ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (member.relation.isNotBlank()) "${member.name}（${member.relation}）" else member.name,
                                            color = if (member.id == selectedMemberId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        scope.launch { SelectedMemberHolder.select(member.id, viewModel.database) }
                                        showMemberMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { navController.navigate("add_record/-1") }) {
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
            if (uiState.records.isNotEmpty()) {
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
                                    value = uiState.records.size.toString(),
                                    label = stringResource(R.string.medical_record_stat_total) + stringResource(R.string.medical_record_stat_current_list),
                                    modifier = Modifier.weight(1f)
                                )
                                StatItem(
                                    value = uiState.records.count {
                                        it.onsetTime.month == java.time.Month.from(java.time.LocalDate.now())
                                    }.toString(),
                                    label = stringResource(R.string.medical_record_stat_month) + stringResource(R.string.medical_record_stat_current_list),
                                    modifier = Modifier.weight(1f)
                                )
                                StatItem(
                                    value = uiState.records.sumOf { it.medItems.size }.toString(),
                                    label = stringResource(R.string.medical_record_stat_medications) + stringResource(R.string.medical_record_stat_current_list),
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
            if (uiState.records.isNotEmpty()) {
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

            // 排序选项
            if (uiState.records.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sortOptions = listOf(
                            "time" to stringResource(R.string.settings_sort_by_time),
                            "hospital" to stringResource(R.string.settings_sort_by_hospital),
                            "diagnosis" to stringResource(R.string.settings_sort_by_diagnosis)
                        )
                        sortOptions.forEach { (key, label) ->
                            FilterChip(
                                selected = sortOrder == key,
                                onClick = { sortOrder = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // 记录列表
            if (uiState.error != null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.medical_record_error_loading, uiState.error ?: ""), color = MaterialTheme.colorScheme.error)
                    }
                }
            } else if (uiState.records.isEmpty()) {
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
                val sortedRecords = when (sortOrder) {
                    "hospital" -> uiState.records.sortedBy { it.hospital.ifBlank { "zzz" } }
                    "diagnosis" -> uiState.records.sortedBy { it.diagnosis.ifBlank { "zzz" } }
                    else -> uiState.records.sortedByDescending { it.onsetTime }
                }
                items(sortedRecords) { record ->
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
                    viewModel.deleteRecord(record)
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
