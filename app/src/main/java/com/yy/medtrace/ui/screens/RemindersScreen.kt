package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yy.medtrace.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.data.settings.UserMode
import com.yy.medtrace.ui.components.EmptyState
import com.yy.medtrace.ui.components.RepeatType
import com.yy.medtrace.ui.components.SectionCard
import com.yy.medtrace.ui.components.SectionHeader
import com.yy.medtrace.ui.components.TodoCategory
import com.yy.medtrace.ui.components.TodoEditSheet
import com.yy.medtrace.ui.components.repeatLabelOrNull
import com.yy.medtrace.ui.components.todoCategoryLabel
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.CardPadding
import com.yy.medtrace.ui.theme.CardPaddingElderly
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.appCardElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.viewmodel.RemindersViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel = hiltViewModel(),
    navController: NavController,
    userModeStore: com.yy.medtrace.data.settings.UserModeStore
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showTodoSheet by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<HealthTodo?>(null) }
    var sheetCategory by remember { mutableStateOf(TodoCategory.OTHER) }

    val currentMode by userModeStore.currentMode.collectAsState()
    val isElderlyMode = currentMode == UserMode.ELDERLY

    LaunchedEffect(Unit) {
        viewModel.loadReminders()
    }

    val members = uiState.members
    val todos = uiState.todos

    Scaffold(
        topBar = {
            GradientTopBar(
                title = stringResource(R.string.screen_reminders_title),
                subtitle = stringResource(R.string.screen_reminders_subtitle),
                actions = {
                    IconButton(onClick = {
                        editingTodo = null
                        sheetCategory = TodoCategory.MEDICATION
                        showTodoSheet = true
                    }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.screen_reminders_add_reminder),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isElderlyMode) 24.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (isElderlyMode) CardPaddingElderly else CardPadding),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
            // 快速添加 - 长辈版隐藏
            if (!isElderlyMode) {
                item {
                    QuickAddSection(onAdd = { category ->
                        sheetCategory = category
                        editingTodo = null
                        showTodoSheet = true
                    })
                }
            }

            // 全部提醒：直接平铺所有人的提醒（已移除"本周计划"与按类型折叠分组）
            if (todos.isEmpty()) {
                item {
                    EmptyReminders(onAdd = {
                        editingTodo = null
                        sheetCategory = TodoCategory.MEDICATION
                        showTodoSheet = true
                    })
                }
            } else {
                item(key = "header_all_reminders") {
                    SectionHeader(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.screen_reminders_all_title),
                        count = todos.size
                    )
                }
                items(todos, key = { "todo_${it.id}" }) { todo ->
                    ReminderItem(
                        todo = todo,
                        onToggle = { viewModel.setTodoDone(todo.id, !todo.done) },
                        onDelete = { viewModel.deleteTodo(todo) },
                        onEdit = { editingTodo = todo },
                        isElderlyMode = isElderlyMode
                    )
                }
            }
        }
        }
    }

    if (showTodoSheet) {
        TodoEditSheet(
            members = members,
            initialCategory = sheetCategory,
            isElderlyMode = isElderlyMode,
            onDismiss = { showTodoSheet = false },
            onSave = { memberId, memberName, content, dueDate, repeatType, category, reminderTime ->
                viewModel.insertTodo(
                    HealthTodo(
                        memberId = memberId,
                        memberName = memberName,
                        content = content,
                        dueDate = dueDate,
                        repeatType = repeatType.storageKey,
                        category = category,
                        reminderTime = reminderTime
                    )
                )
                showTodoSheet = false
            }
        )
    }

    editingTodo?.let { todo ->
        TodoEditSheet(
            members = members,
            todo = todo,
            isElderlyMode = isElderlyMode,
            onDismiss = { editingTodo = null },
            onSave = { _, _, content, dueDate, repeatType, category, reminderTime ->
                viewModel.updateTodo(
                    todo.copy(
                        content = content,
                        category = category,
                        dueDate = dueDate,
                        repeatType = repeatType.storageKey,
                        reminderTime = reminderTime
                    )
                )
                editingTodo = null
            }
        )
    }
}

@Composable
private fun EmptyReminders(onAdd: () -> Unit) {
    EmptyState(
        icon = Icons.Default.DateRange,
        title = stringResource(R.string.screen_reminders_empty_title),
        hint = stringResource(R.string.screen_reminders_empty_hint),
        actionText = stringResource(R.string.screen_reminders_add_reminder),
        onAction = onAdd
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ReminderItem(
    todo: HealthTodo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    isElderlyMode: Boolean = false
) {
    val isOverdue = todo.dueDate.isBefore(LocalDate.now()) && !todo.done
    val repeatLabel = repeatLabelOrNull(RepeatType.fromKey(todo.repeatType))
    var showActions by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { showActions = !showActions }
            .padding(vertical = if (isElderlyMode) 8.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.done,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(if (isElderlyMode) 32.dp else 24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                todo.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (isElderlyMode) 18.sp
                    else MaterialTheme.typography.bodyMedium.fontSize
                ),
                color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (todo.done) TextDecoration.LineThrough else null
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(if (isElderlyMode) 16.dp else 12.dp),
                    tint = if (isOverdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    todo.dueDate.format(DateTimeFormatter.ofPattern("MM-dd")),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (isElderlyMode) 14.sp
                        else MaterialTheme.typography.labelSmall.fontSize
                    ),
                    color = if (isOverdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (repeatLabel != null) {
                    Surface(
                        shape = AppShapes.extraSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            repeatLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (isElderlyMode) 14.sp
                                else MaterialTheme.typography.labelSmall.fontSize
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    todo.memberName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (isElderlyMode) 14.sp
                        else MaterialTheme.typography.labelSmall.fontSize
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showActions) {
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.screen_reminders_edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.screen_reminders_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            IconButton(onClick = { showActions = true }, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.cd_more_actions),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickAddSection(onAdd: (TodoCategory) -> Unit) {
    SectionCard(title = stringResource(R.string.screen_reminders_quick_add)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TodoCategory.entries.forEach { category ->
                QuickAddChip(
                    icon = category.icon,
                    label = todoCategoryLabel(category),
                    onClick = { onAdd(category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickAddChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        shape = AppShapes.medium,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

