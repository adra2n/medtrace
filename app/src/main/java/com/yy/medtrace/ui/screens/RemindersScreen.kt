package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
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
import com.yy.medtrace.ui.components.TodoCategory
import com.yy.medtrace.ui.components.TodoEditSheet
import com.yy.medtrace.ui.components.repeatLabelOrNull
import com.yy.medtrace.ui.components.todoCategoryFromStored
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
    var expandedTypes by remember { mutableStateOf(setOf<String>()) }

    val currentMode by userModeStore.currentMode.collectAsState()
    val isElderlyMode = currentMode == UserMode.ELDERLY

    LaunchedEffect(Unit) {
        viewModel.loadReminders()
    }

    val members = uiState.members
    val todos = uiState.todos

    val groupedByType = remember(todos) { todos.groupBy { it.category } }

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

            // 本周计划 - 长辈版隐藏
            if (!isElderlyMode) {
                item { WeeklyPlanSection(todos = todos) }
            }

            if (groupedByType.isEmpty()) {
                item {
                    EmptyReminders(onAdd = {
                        editingTodo = null
                        sheetCategory = TodoCategory.MEDICATION
                        showTodoSheet = true
                    })
                }
            } else {
                groupedByType.forEach { (type, typeTodos) ->
                    val isExpanded = if (isElderlyMode) true else expandedTypes.contains(type)
                    item(key = "type_$type") {
                        ReminderTypeGroup(
                            type = type,
                            todos = typeTodos,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedTypes = if (isExpanded) expandedTypes - type
                                else expandedTypes + type
                            },
                            onToggle = { todo, done -> viewModel.setTodoDone(todo.id, done) },
                            onDelete = { viewModel.deleteTodo(it) },
                            onEdit = { editingTodo = it },
                            isElderlyMode = isElderlyMode
                        )
                    }
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
private fun ReminderTypeGroup(
    type: String,
    todos: List<HealthTodo>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggle: (HealthTodo, Boolean) -> Unit,
    onDelete: (HealthTodo) -> Unit,
    onEdit: (HealthTodo) -> Unit,
    isElderlyMode: Boolean = false
) {
    val pendingCount = todos.count { !it.done }
    val category = todoCategoryFromStored(type)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = appCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(if (isElderlyMode) CardPaddingElderly else CardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isElderlyMode) 40.dp else 32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        category.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isElderlyMode) 24.dp else 18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        type,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = if (isElderlyMode) 20.sp
                            else MaterialTheme.typography.titleSmall.fontSize
                        ),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.screen_reminders_pending_count, todos.size, pendingCount),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = if (isElderlyMode) 16.sp
                            else MaterialTheme.typography.labelSmall.fontSize
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(R.string.cd_toggle_expand),
                    modifier = Modifier.size(if (isElderlyMode) 28.dp else 20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                todos.forEach { todo ->
                    ReminderItem(
                        todo = todo,
                        onToggle = { onToggle(todo, !todo.done) },
                        onDelete = { onDelete(todo) },
                        onEdit = { onEdit(todo) },
                        isElderlyMode = isElderlyMode
                    )
                }
            }
        }
    }
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun WeeklyPlanSection(todos: List<HealthTodo>) {
    val today = LocalDate.now()
    val weekEnd = today.plusDays(6)
    val weeklyTodos = todos.filter {
        !it.done && it.dueDate in today..weekEnd
    }.sortedBy { it.dueDate }

    SectionCard(title = stringResource(R.string.screen_reminders_weekly_plan)) {
        if (weeklyTodos.isEmpty()) {
            Text(
                stringResource(R.string.screen_reminders_no_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            weeklyTodos.take(5).forEach { todo ->
                val dayLabel = when (todo.dueDate) {
                    today -> stringResource(R.string.screen_reminders_today)
                    today.plusDays(1) -> stringResource(R.string.screen_reminders_tomorrow)
                    today.plusDays(2) -> stringResource(R.string.screen_reminders_day_after_tomorrow)
                    else -> {
                        when (todo.dueDate.dayOfWeek) {
                            java.time.DayOfWeek.MONDAY -> stringResource(R.string.screen_reminders_monday)
                            java.time.DayOfWeek.TUESDAY -> stringResource(R.string.screen_reminders_tuesday)
                            java.time.DayOfWeek.WEDNESDAY -> stringResource(R.string.screen_reminders_wednesday)
                            java.time.DayOfWeek.THURSDAY -> stringResource(R.string.screen_reminders_thursday)
                            java.time.DayOfWeek.FRIDAY -> stringResource(R.string.screen_reminders_friday)
                            java.time.DayOfWeek.SATURDAY -> stringResource(R.string.screen_reminders_saturday)
                            java.time.DayOfWeek.SUNDAY -> stringResource(R.string.screen_reminders_sunday)
                            else -> todo.dueDate.format(DateTimeFormatter.ofPattern("MM-dd"))
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        dayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (todo.dueDate == today) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(40.dp)
                    )
                    Text(
                        todo.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        todo.memberName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (weeklyTodos.size > 5) {
                Text(
                    stringResource(R.string.screen_reminders_more_items, weeklyTodos.size - 5),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
