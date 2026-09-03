package com.yy.medtrace.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MedicalServices
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
import com.yy.medtrace.ui.components.SectionHeader
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.CardPadding
import com.yy.medtrace.ui.theme.CardPaddingElderly
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.ScreenHorizontalPadding
import com.yy.medtrace.ui.theme.ScreenHorizontalPaddingElderly
import com.yy.medtrace.ui.theme.appCardElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.viewmodel.MedicalRecordViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    var showAddRecordSheet by remember { mutableStateOf(false) }

    val currentMode by userModeStore.currentMode.collectAsState()
    val isElderlyMode = currentMode == UserMode.ELDERLY

    // 就诊只记录到日期，不展示时分
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    // 「记录」页展示所有人的记录：memberId = null 走全量查询（不按成员过滤）。
    LaunchedEffect(Unit) {
        viewModel.loadMembers()
        viewModel.loadRecords(memberId = null)
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = stringResource(R.string.medical_record_title),
                subtitle = if (uiState.records.isNotEmpty()) {
                    stringResource(R.string.medical_record_subtitle_count, uiState.records.size)
                } else null,
                actions = {
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
        // 统一按就诊时间倒序（最新的排最前）。记录再多也会在滚动到底部时通过
        // loadMore() 增量加载，排序随之覆盖完整数据集。
        val sortedRecords = remember(uiState.records) {
            uiState.records.sortedByDescending { it.onsetTime }
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
                            title = stringResource(R.string.medical_record_empty_no_records_all),
                            hint = stringResource(R.string.medical_record_empty_hint),
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                }

                else -> {
                    // 分区标题：与「提醒」页保持一致的结构（图标 + 标题 + 数量徽标）
                    item {
                        SectionHeader(
                            icon = Icons.Default.MedicalServices,
                            title = stringResource(R.string.medical_record_all_title),
                            count = uiState.records.size
                        )
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

