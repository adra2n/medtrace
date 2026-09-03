package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yy.medtrace.R
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.ui.components.EmptyRecordsState
import com.yy.medtrace.ui.components.MemberEditDialog
import com.yy.medtrace.ui.components.MedicalRecordCard
import com.yy.medtrace.ui.components.SectionCard
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
        scope.launch {
            SelectedMemberHolder.select(memberId, database)
            member = database.familyMemberDao().getMemberById(memberId)
        }
        database.medicalRecordDao().getRecordsByMember(memberId)
            .catch { }
            .collect { records = it }
        database.healthTodoDao().getAll()
            .catch { }
            .collect { list -> reminders = list.filter { it.memberId == memberId } }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp, top = 16.dp)
        ) {
            // 就诊记录（合并原「用药/检查报告/检查指标/就诊记录」分类，直接平铺）
            item {
                SectionCard(title = stringResource(R.string.tab_visit_records)) {
                    if (records.isEmpty()) {
                        EmptyRecordsState()
                    }
                }
            }
            if (records.isNotEmpty()) {
                items(records, key = { it.id }) { record ->
                    MedicalRecordCard(record, showActions = false)
                }
            }

            // 后续提醒
            item {
                SectionCard(title = stringResource(R.string.member_detail_section_reminders)) {
                    if (reminders.isEmpty()) {
                        Text(
                            stringResource(R.string.member_detail_no_reminders),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (reminders.isNotEmpty()) {
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    todo.content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${todo.dueDate} ${todo.reminderTime} · ${todo.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (todo.done) {
                Text(
                    stringResource(R.string.member_detail_reminder_done),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
