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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yy.medtrace.R
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.data.settings.UserMode
import com.yy.medtrace.ui.components.AddRecordBottomSheet
import com.yy.medtrace.ui.components.EmptyState
import com.yy.medtrace.ui.components.MedicalRecordCard
import com.yy.medtrace.ui.state.SelectedMemberHolder
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.CardPadding
import com.yy.medtrace.ui.theme.CardPaddingElderly
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.ScreenHorizontalPadding
import com.yy.medtrace.ui.theme.ScreenHorizontalPaddingElderly
import com.yy.medtrace.ui.theme.appCardElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.viewmodel.MedicalRecordViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val SEARCH_DEBOUNCE_MS = 300L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordScreen(
    viewModel: MedicalRecordViewModel = hiltViewModel(),
    navController: NavController,
    userModeStore: com.yy.medtrace.data.settings.UserModeStore
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var pendingDelete by remember { mutableStateOf<MedicalRecord?>(null) }
    var keyword by remember { mutableStateOf("") }
    // 输入防抖：避免每敲一个字都查一次数据库
    val debouncedKeyword by produceState(initialValue = "", keyword) {
        delay(SEARCH_DEBOUNCE_MS)
        value = keyword
    }
    var fromDate by remember { mutableStateOf<LocalDate?>(null) }
    var toDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf(DateTarget.From) }
    var showFilter by remember { mutableStateOf(false) }
    var showMemberMenu by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf("time") }
    var showAddRecordSheet by remember { mutableStateOf(false) }
    val selectedMemberId = SelectedMemberHolder.selectedMemberId.value

    val currentMode by userModeStore.currentMode.collectAsState()
    val isElderlyMode = currentMode == UserMode.ELDERLY

    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    LaunchedEffect(Unit) {
        viewModel.loadMembers()
        if (SelectedMemberHolder.selectedMemberId.value == null) {
            val latest = viewModel.getLatestRecord()
            val defaultMember = viewModel.getDefaultMember()
            val fallbackId = latest?.patientId ?: defaultMember?.id ?: 1L
            scope.launch { SelectedMemberHolder.select(fallbackId, viewModel.database) }
        }
    }

    // 筛选条件变化时重新加载第一页
    LaunchedEffect(selectedMemberId, debouncedKeyword, fromDate, toDate) {
        val id = selectedMemberId
        if (id != null) {
            viewModel.loadRecords(
                memberId = id,
                keyword = debouncedKeyword.trim().takeIf { it.isNotEmpty() },
                fromDate = fromDate.atStartOfDayMillis(),
                toDate = toDate.atEndOfDayMillis()
            )
        } else {
            viewModel.loadRecords(0)
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = stringResource(R.string.medical_record_title),
                subtitle = if (uiState.records.isNotEmpty()) {
                    stringResource(R.string.medical_record_subtitle_count, uiState.records.size)
                } else null,
                actions = {
                    Box {
                        IconButton(onClick = { showMemberMenu = true }) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = stringResource(R.string.medical_record_cd_select_member),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showMemberMenu,
                            onDismissRequest = { showMemberMenu = false }
                        ) {
                            uiState.members.forEach { member ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (member.relation.isNotBlank()) {
                                                "${member.name}（${member.relation}）"
                                            } else member.name,
                                            color = if (member.id == selectedMemberId) {
                                                MaterialTheme.colorScheme.primary
                                            } else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        scope.launch {
                                            SelectedMemberHolder.select(member.id, viewModel.database)
                                        }
                                        showMemberMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showAddRecordSheet = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.medical_record_cd_add),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        val listState = rememberLazyListState()
        val isRefreshing by viewModel.isRefreshing.collectAsState()
        // 排序作用于已加载（经分页增量合并）的全部记录；数据库里有更多记录时
        // 会在滚动到底部时通过 loadMore() 继续加载，排序随之覆盖完整数据集。
        val sortedRecords = remember(uiState.records, sortOrder) {
            when (sortOrder) {
                "hospital" -> uiState.records.sortedBy { it.hospital.ifBlank { "zzz" } }
                "diagnosis" -> uiState.records.sortedBy { it.diagnosis.ifBlank { "zzz" } }
                else -> uiState.records.sortedByDescending { it.onsetTime }
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    if (isElderlyMode) ScreenHorizontalPaddingElderly else ScreenHorizontalPadding
                ),
                verticalArrangement = Arrangement.spacedBy(
                    if (isElderlyMode) CardPaddingElderly else CardPadding
                ),
            ) {
            // 搜索筛选 - 长辈版简化
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.large,
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                    elevation = appCardElevation()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (isElderlyMode) 16.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isElderlyMode) 22.dp else 18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.medical_record_search_filter),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = if (isElderlyMode) 18.sp
                                    else MaterialTheme.typography.titleSmall.fontSize
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    stringResource(R.string.medical_record_search_placeholder),
                                    style = if (isElderlyMode) MaterialTheme.typography.bodyLarge
                                    else MaterialTheme.typography.bodyMedium
                                )
                            },
                            singleLine = true,
                            trailingIcon = {
                                Row {
                                    if (keyword.isNotEmpty()) {
                                        IconButton(onClick = { keyword = "" }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.medical_record_cd_clear)
                                            )
                                        }
                                    }
                                    if (!isElderlyMode) {
                                        IconButton(onClick = { showFilter = !showFilter }) {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = stringResource(R.string.medical_record_cd_filter),
                                                tint = if (showFilter || fromDate != null || toDate != null) {
                                                    MaterialTheme.colorScheme.primary
                                                } else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = stringResource(R.string.medical_record_cd_search)
                                )
                            },
                            textStyle = if (isElderlyMode) MaterialTheme.typography.bodyLarge
                            else MaterialTheme.typography.bodyMedium
                        )
                        if (!isElderlyMode && showFilter) {
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

            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                }

                uiState.error != null -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(
                                    R.string.medical_record_error_loading,
                                    uiState.error ?: ""
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                uiState.records.isEmpty() -> {
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
                }

                else -> {
                    // 记录列表标题
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isElderlyMode) 24.dp else 20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.medical_record_list_title),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = if (isElderlyMode) 18.sp
                                    else MaterialTheme.typography.titleSmall.fontSize
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 排序选项 - 长辈版隐藏
                    if (!isElderlyMode) {
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

                    items(sortedRecords, key = { it.id }) { record ->
                        MedicalRecordCard(
                            record = record,
                            dateFormatter = dateFormatter,
                            onEdit = { navController.navigate("add_record/${record.id}") },
                            onDelete = { pendingDelete = record }
                        )
                    }

                    // 分页状态：加载中 / 已全部加载
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                uiState.isLoadingMore -> CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )

                                !uiState.hasMore -> Text(
                                    stringResource(
                                        R.string.medical_record_all_loaded,
                                        uiState.records.size
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        // 滚动到底部附近时加载下一页
        val shouldLoadMore by remember {
            derivedStateOf {
                val lastVisibleItem =
                    listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalItems = listState.layoutInfo.totalItemsCount
                lastVisibleItem >= totalItems - 3 &&
                    !uiState.isLoadingMore &&
                    !uiState.isLoading &&
                    uiState.hasMore
            }
        }

        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore) viewModel.loadMore()
        }
    }

    if (showAddRecordSheet) {
        AddRecordBottomSheet(
            onDismiss = { showAddRecordSheet = false },
            onNext = { memberId, visitType, diagnosis, hospital, onsetTime ->
                showAddRecordSheet = false
                navController.navigate(
                    "add_record/-1?memberId=$memberId&diagnosis=$diagnosis" +
                        "&hospital=$hospital&onsetTime=$onsetTime&visitType=$visitType"
                )
            },
            members = uiState.members
        )
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
            text = {
                Text(
                    stringResource(
                        R.string.medical_record_dialog_delete_message,
                        record.diagnosis
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(record)
                    pendingDelete = null
                }) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

private fun LocalDate?.atStartOfDayMillis(): Long? =
    this?.atStartOfDay()?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

private fun LocalDate?.atEndOfDayMillis(): Long? =
    this?.atTime(23, 59, 59)?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

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
        label = { Text(if (value != null) "$label：$value" else label) },
        modifier = modifier
    )
}
