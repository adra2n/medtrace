package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yy.medtrace.R
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.ui.components.MemberEditDialog
import com.yy.medtrace.ui.components.MedicalRecordCard
import com.yy.medtrace.ui.state.SelectedMemberHolder
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.ui.theme.computeAge
import com.yy.medtrace.viewmodel.MemberDetailViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    viewModel: MemberDetailViewModel = hiltViewModel(),
    navController: NavController,
    memberId: Long,
    initialTab: String? = null
) {
    val database = viewModel.database
    var member by remember { mutableStateOf<FamilyMember?>(null) }
    var records by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var reminders by remember { mutableStateOf<List<HealthTodo>>(emptyList()) }
    var showEdit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(memberId) {
        SelectedMemberHolder.select(memberId, database)
        member = database.familyMemberDao().getMemberById(memberId)
        val isDefault = member?.isDefault == true

        launch {
            database.medicalRecordDao().getRecordsByMember(memberId)
                .catch { }
                .collect { records = it }
        }
        launch {
            database.healthTodoDao().getAll()
                .catch { }
                .collect { list ->
                    reminders = list.filter {
                        it.memberId == memberId || (isDefault && it.memberId == 0L)
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = member?.let {
                    val age = computeAge(it.birthday)?.let { a -> "，${a}岁" } ?: ""
                    "${it.name}$age"
                } ?: "成员档案",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showEdit = true }) {
                        Text("编辑档案", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 成员概览卡片
            item {
                MemberSummaryCard(member = member, recordCount = records.size, reminderCount = reminders.size)
            }

            // 就诊记录
            item {
                SectionHeader(
                    icon = Icons.AutoMirrored.Filled.EventNote,
                    title = stringResource(R.string.tab_visit_records),
                    count = records.size
                )
            }
            if (records.isEmpty()) {
                item {
                    EmptyRow(text = "暂无就诊记录")
                }
            } else {
                items(records, key = { it.id }) { record ->
                    MedicalRecordCard(
                        record = record,
                        showActions = false,
                        showPatientName = false
                    )
                }
            }

            // 后续提醒
            item {
                SectionHeader(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.member_detail_section_reminders),
                    count = reminders.size
                )
            }
            if (reminders.isEmpty()) {
                item {
                    EmptyRow(text = stringResource(R.string.member_detail_no_reminders))
                }
            } else {
                items(reminders, key = { it.id }) { todo ->
                    ReminderItemCard(todo)
                }
            }
        }
    }

    if (showEdit) {
        MemberEditDialog(
            member = member,
            onDismiss = { showEdit = false },
            onSave = { m ->
                scope.launch {
                    if (member == null) database.familyMemberDao().insert(m)
                    else database.familyMemberDao().update(m)
                    member = m
                }
                showEdit = false
            }
        )
    }
}

@Composable
private fun MemberSummaryCard(
    member: FamilyMember?,
    recordCount: Int,
    reminderCount: Int,
    modifier: Modifier = Modifier
) {
    if (member == null) return
    Card(
        modifier = modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = member.name.take(1),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            append(member.relation.ifBlank { "家庭成员" })
                            computeAge(member.birthday)?.let { append(" · ${it}岁") }
                            if (member.gender.isNotBlank()) append(" · ${member.gender}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStat(title = "就诊记录", value = recordCount.toString(), modifier = Modifier.weight(1f))
                SummaryStat(title = "后续提醒", value = reminderCount.toString(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryStat(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = AppShapes.small,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        if (action != null) {
            action()
        }
    }
}

@Composable
private fun EmptyRow(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ReminderItemCard(todo: HealthTodo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (todo.done) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (todo.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${todo.dueDate} ${todo.reminderTime} · ${todo.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (todo.done) {
                Surface(
                    shape = AppShapes.small,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = stringResource(R.string.member_detail_reminder_done),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
