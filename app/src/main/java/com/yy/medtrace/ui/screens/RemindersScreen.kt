package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yy.medtrace.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.data.settings.UserMode
import com.yy.medtrace.data.settings.UserModeStore
import com.yy.medtrace.ui.components.EmptyState
import com.yy.medtrace.ui.components.SectionCard
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.ui.theme.Reminder
import com.yy.medtrace.ui.theme.CardPadding
import com.yy.medtrace.ui.theme.CardPaddingElderly
import com.yy.medtrace.viewmodel.MonthlyStats
import com.yy.medtrace.viewmodel.RemindersViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<HealthTodo?>(null) }
    var expandedTypes by remember { mutableStateOf(setOf<String>()) }
    
    val userModeStore = remember { UserModeStore(context) }
    val currentMode by userModeStore.currentMode.collectAsState()
    val isElderlyMode = currentMode == UserMode.ELDERLY

    LaunchedEffect(Unit) {
        viewModel.loadReminders()
    }

    val members = uiState.members
    val todos = uiState.todos
    val stats = uiState.monthlyStats

    // 按类型分组
    val groupedByType = remember(todos) {
        todos.groupBy { it.category }
    }

    val typeIcons = mapOf(
        "用药" to "💊",
        "复查" to "🏥",
        "检查" to "🔬",
        "其他" to "📋"
    )

    var selectedCategory by remember { mutableStateOf(context.getString(R.string.screen_reminders_category_other)) }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = stringResource(R.string.screen_reminders_title),
                subtitle = stringResource(R.string.screen_reminders_subtitle),
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, stringResource(R.string.screen_reminders_add_reminder), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = if (isElderlyMode) 24.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isElderlyMode) CardPaddingElderly else CardPadding),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // 快速添加 - 长辈版隐藏
            if (!isElderlyMode) {
                item {
                    QuickAddSection(
                        onAdd = { category ->
                            selectedCategory = category
                            showAddDialog = true
                        }
                    )
                }
            }

            // 本周计划 - 长辈版隐藏
            if (!isElderlyMode) {
                item {
                    WeeklyPlanSection(todos = todos)
                }
            }

            // 按类型分组的提醒列表 - 长辈版简化
            if (groupedByType.isEmpty()) {
                item {
                    EmptyReminders(onAdd = { showAddDialog = true })
                }
            } else {
                groupedByType.forEach { (type, typeTodos) ->
                    val isExpanded = if (isElderlyMode) true else expandedTypes.contains(type)
                    item(key = "type_$type") {
                        ReminderTypeGroup(
                            type = type,
                            icon = typeIcons[type] ?: "📋",
                            todos = typeTodos,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedTypes = if (isExpanded) expandedTypes - type
                                else expandedTypes + type
                            },
                            onToggle = { todo, done ->
                                viewModel.setTodoDone(todo.id, done)
                            },
                            onDelete = { todo ->
                                viewModel.deleteTodo(todo)
                            },
                            onEdit = { todo ->
                                editingTodo = todo
                            },
                            isElderlyMode = isElderlyMode
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTodoDialog(
            members = members,
            initialCategory = selectedCategory,
            onDismiss = { showAddDialog = false },
            onSave = { memberId, memberName, content, dueDate, repeatType, category, reminderTime ->
                viewModel.insertTodo(
                    HealthTodo(
                        memberId = memberId,
                        memberName = memberName,
                        content = content,
                        dueDate = dueDate,
                        repeatType = repeatType,
                        category = category,
                        reminderTime = reminderTime
                    )
                )
                showAddDialog = false
            }
        )
    }

    editingTodo?.let { todo ->
        EditTodoDialog(
            todo = todo,
            onDismiss = { editingTodo = null },
            onUpdate = { content, category, dueDate, repeatType, reminderTime ->
                viewModel.updateTodo(todo.copy(
                    content = content,
                    category = category,
                    dueDate = dueDate,
                    repeatType = repeatType,
                    reminderTime = reminderTime
                ))
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
    icon: String,
    todos: List<HealthTodo>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggle: (HealthTodo, Boolean) -> Unit,
    onDelete: (HealthTodo) -> Unit,
    onEdit: (HealthTodo) -> Unit,
    isElderlyMode: Boolean = false
) {
    val pendingCount = todos.count { !it.done }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(if (isElderlyMode) CardPaddingElderly else CardPadding)
        ) {
            // 头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (isElderlyMode) 24.sp else MaterialTheme.typography.titleMedium.fontSize
                ))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        type,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = if (isElderlyMode) 20.sp else MaterialTheme.typography.titleSmall.fontSize
                        ),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.screen_reminders_pending_count, todos.size, pendingCount),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = if (isElderlyMode) 16.sp else MaterialTheme.typography.labelSmall.fontSize
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(if (isElderlyMode) 28.dp else 20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 展开内容
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
    val repeatLabel = RemindersViewModel.repeatLabel(todo.repeatType)
    var showActions by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                    fontSize = if (isElderlyMode) 18.sp else MaterialTheme.typography.bodyMedium.fontSize
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
                        fontSize = if (isElderlyMode) 14.sp else MaterialTheme.typography.labelSmall.fontSize
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
                                fontSize = if (isElderlyMode) 14.sp else MaterialTheme.typography.labelSmall.fontSize
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    todo.memberName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (isElderlyMode) 14.sp else MaterialTheme.typography.labelSmall.fontSize
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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTodoDialog(
    todo: HealthTodo,
    onDismiss: () -> Unit,
    onUpdate: (content: String, category: String, dueDate: LocalDate, repeatType: String, reminderTime: String) -> Unit
) {
    var content by remember { mutableStateOf(todo.content) }
    var category by remember { mutableStateOf(todo.category) }
    var dueDate by remember { mutableStateOf(todo.dueDate) }
    var repeatEnabled by remember { mutableStateOf(todo.repeatType != "none") }
    var selectedType by remember { mutableStateOf(if (todo.repeatType == "none") "day" else todo.repeatType) }
    var showDatePicker by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf(todo.reminderTime) }
    var showTimePicker by remember { mutableStateOf(false) }

    val categories = listOf(
        stringResource(R.string.screen_reminders_category_medication) to "💊",
        stringResource(R.string.screen_reminders_category_followup) to "🏥",
        stringResource(R.string.screen_reminders_category_examination) to "🔬",
        stringResource(R.string.screen_reminders_category_other) to "📋"
    )
    val units = listOf(
        "day" to stringResource(R.string.screen_reminders_unit_day),
        "week" to stringResource(R.string.screen_reminders_unit_week)
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.screen_reminders_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.screen_reminders_cancel)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val parts = reminderTime.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.screen_home_select_reminder_time)) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hour = timePickerState.hour.toString().padStart(2, '0')
                        val minute = timePickerState.minute.toString().padStart(2, '0')
                        reminderTime = "$hour:$minute"
                        showTimePicker = false
                    }
                ) { Text(stringResource(R.string.screen_home_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.screen_home_cancel)) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.screen_reminders_edit_reminder)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 提醒类别
                Text(stringResource(R.string.screen_reminders_category_label), style = MaterialTheme.typography.labelMedium)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 第一行：用药、复查
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.take(2).forEach { (type, icon) ->
                            FilterChip(
                                selected = category == type,
                                onClick = { category = type },
                                label = { Text("$icon$type") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // 第二行：检查、其他
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.drop(2).forEach { (type, icon) ->
                            FilterChip(
                                selected = category == type,
                                onClick = { category = type },
                                label = { Text("$icon$type") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 提醒内容
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.screen_reminders_content_label)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // 日期
                Text(stringResource(R.string.screen_reminders_date_label), style = MaterialTheme.typography.labelMedium)
                val dateLabel = dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.screen_reminders_planned_date)) },
                    trailingIcon = { Icon(Icons.Default.DateRange, stringResource(R.string.screen_reminders_select_date), tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    enabled = false
                )

                // 提醒时间
                OutlinedTextField(
                    value = reminderTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.screen_home_reminder_time)) },
                    trailingIcon = { Icon(Icons.Default.DateRange, stringResource(R.string.screen_home_select_time), tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    enabled = false
                )

                // 重复设置
                Text(stringResource(R.string.screen_reminders_repeat), style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = !repeatEnabled,
                        onClick = { repeatEnabled = false },
                        label = { Text(stringResource(R.string.screen_reminders_no_repeat)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = repeatEnabled,
                        onClick = { repeatEnabled = true },
                        label = { Text(stringResource(R.string.screen_reminders_repeat)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (repeatEnabled) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        units.forEach { (type, label) ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val repeatType = if (repeatEnabled) selectedType else "none"
                    onUpdate(content, category, dueDate, repeatType, reminderTime)
                },
                enabled = content.isNotBlank()
            ) { Text(stringResource(R.string.screen_reminders_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.screen_reminders_cancel)) }
        }
    )
}

@Composable
private fun QuickAddSection(onAdd: (String) -> Unit) {
    val context = LocalContext.current
    SectionCard(title = stringResource(R.string.screen_reminders_quick_add)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickAddChip(
                icon = "💊",
                label = stringResource(R.string.screen_reminders_category_medication),
                onClick = { onAdd(context.getString(R.string.screen_reminders_category_medication)) },
                modifier = Modifier.weight(1f)
            )
            QuickAddChip(
                icon = "🏥",
                label = stringResource(R.string.screen_reminders_category_followup),
                onClick = { onAdd(context.getString(R.string.screen_reminders_category_followup)) },
                modifier = Modifier.weight(1f)
            )
            QuickAddChip(
                icon = "🔬",
                label = stringResource(R.string.screen_reminders_category_examination),
                onClick = { onAdd(context.getString(R.string.screen_reminders_category_examination)) },
                modifier = Modifier.weight(1f)
            )
            QuickAddChip(
                icon = "📋",
                label = stringResource(R.string.screen_reminders_category_other),
                onClick = { onAdd(context.getString(R.string.screen_reminders_category_other)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickAddChip(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        shape = AppShapes.medium,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
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
    val today = java.time.LocalDate.now()
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
                        val dayOfWeek = todo.dueDate.dayOfWeek
                        when (dayOfWeek) {
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
                        color = if (todo.dueDate == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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