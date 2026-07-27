package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.ui.components.EmptyState
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.Primary
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.viewmodel.MonthlyStats
import com.yy.medtrace.viewmodel.RemindersViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<HealthTodo?>(null) }
    var expandedTypes by remember { mutableStateOf(setOf<String>()) }

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

    var selectedCategory by remember { mutableStateOf("其他") }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "提醒管理",
                subtitle = "查看和管理所有提醒",
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "添加提醒", tint = Color.White)
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
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // 月度统计
            item {
                MonthlyStatsCard(stats = stats)
            }

            // 快速添加
            item {
                QuickAddSection(
                    onAdd = { category ->
                        selectedCategory = category
                        showAddDialog = true
                    }
                )
            }

            // 本周计划
            item {
                WeeklyPlanSection(todos = todos)
            }

            // 按类型分组的提醒列表
            if (groupedByType.isEmpty()) {
                item {
                    EmptyReminders(onAdd = { showAddDialog = true })
                }
            } else {
                groupedByType.forEach { (type, typeTodos) ->
                    val isExpanded = expandedTypes.contains(type)
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
                            }
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
            onSave = { memberId, memberName, content, dueDate, repeatType, repeatInterval, category ->
                viewModel.insertTodo(
                    HealthTodo(
                        memberId = memberId,
                        memberName = memberName,
                        content = content,
                        dueDate = dueDate,
                        repeatType = repeatType,
                        repeatInterval = repeatInterval,
                        category = category
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
            onUpdate = { content, category, dueDate, repeatType, repeatInterval ->
                viewModel.updateTodo(todo.copy(
                    content = content,
                    category = category,
                    dueDate = dueDate,
                    repeatType = repeatType,
                    repeatInterval = repeatInterval
                ))
                editingTodo = null
            }
        )
    }
}

@Composable
private fun EmptyReminders(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = AppShapes.large,
            color = Primary.copy(alpha = 0.12f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("📋", style = MaterialTheme.typography.headlineLarge)
            }
        }
        EmptyState(
            icon = Icons.Default.DateRange,
            title = "暂无提醒",
            hint = "点击右上角添加用药、复查等提醒",
            action = {
                Button(onClick = onAdd) {
                    Text("添加提醒")
                }
            }
        )
    }
}

@Composable
private fun MonthlyStatsCard(stats: MonthlyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "本月统计",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 完成率
                Column {
                    Text(
                        "${(stats.completionRate * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Text("完成率", style = MaterialTheme.typography.labelSmall)
                }

                // 连续天数
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${stats.streak}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    Text("连续天数", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(12.dp))

            // 进度条
            LinearProgressIndicator(
                progress = { stats.completionRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Primary,
                trackColor = Primary.copy(alpha = 0.12f)
            )

            Spacer(Modifier.height(8.dp))

            // 统计详情
            Text(
                "总计 ${stats.total}项 · 完成 ${stats.completed}项 · 逾期 ${stats.overdue}项",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
    onEdit: (HealthTodo) -> Unit
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
                .padding(12.dp)
        ) {
            // 头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        type,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${todos.size}项 · ${pendingCount}待办",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
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
                        onEdit = { onEdit(todo) }
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
    onEdit: () -> Unit
) {
    val isOverdue = todo.dueDate.isBefore(LocalDate.now()) && !todo.done
    val repeatLabel = RemindersViewModel.repeatLabel(todo.repeatType, todo.repeatInterval)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.done,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = Primary)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                todo.content,
                style = MaterialTheme.typography.bodyMedium,
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
                    modifier = Modifier.size(12.dp),
                    tint = if (isOverdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    todo.dueDate.format(DateTimeFormatter.ofPattern("MM-dd")),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (repeatLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            repeatLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    todo.memberName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // 编辑按钮
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "编辑",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        // 删除按钮
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RepeatEditDialog(
    todo: HealthTodo,
    onDismiss: () -> Unit,
    onConfirm: (type: String, interval: Int) -> Unit
) {
    var repeatEnabled by remember { mutableStateOf(todo.repeatType != "none") }
    var selectedType by remember { mutableStateOf(if (todo.repeatType == "none") "day" else todo.repeatType) }
    var interval by remember { mutableIntStateOf(if (todo.repeatInterval < 1) 1 else todo.repeatInterval) }

    val units = listOf("day" to "天", "week" to "周", "month" to "月", "year" to "年")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改重复设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = todo.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = !repeatEnabled,
                        onClick = { repeatEnabled = false },
                        label = { Text("不重复") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = repeatEnabled,
                        onClick = { repeatEnabled = true },
                        label = { Text("重复") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
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
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("每")
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$interval",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = units.firstOrNull { it.first == selectedType }?.second ?: "",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val type = if (repeatEnabled) selectedType else "none"
                onConfirm(type, interval)
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTodoDialog(
    todo: HealthTodo,
    onDismiss: () -> Unit,
    onUpdate: (content: String, category: String, dueDate: LocalDate, repeatType: String, repeatInterval: Int) -> Unit
) {
    var content by remember { mutableStateOf(todo.content) }
    var category by remember { mutableStateOf(todo.category) }
    var dueDate by remember { mutableStateOf(todo.dueDate) }
    var repeatEnabled by remember { mutableStateOf(todo.repeatType != "none") }
    var selectedType by remember { mutableStateOf(if (todo.repeatType == "none") "day" else todo.repeatType) }
    var interval by remember { mutableIntStateOf(if (todo.repeatInterval < 1) 1 else todo.repeatInterval) }
    var showDatePicker by remember { mutableStateOf(false) }

    val categories = listOf("用药" to "💊", "复查" to "🏥", "检查" to "🔬", "其他" to "📋")
    val units = listOf("day" to "天", "week" to "周", "month" to "月", "year" to "年")

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
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑提醒") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 提醒类别
                Text("提醒类别", style = MaterialTheme.typography.labelMedium)
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
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color.White
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
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color.White
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
                    label = { Text("提醒内容") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 日期
                Text("提醒日期", style = MaterialTheme.typography.labelMedium)
                val dateLabel = dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("计划日期") },
                    trailingIcon = { Icon(Icons.Default.DateRange, "选择日期", tint = Primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = Primary,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    enabled = false
                )

                // 重复设置
                Text("重复", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = !repeatEnabled,
                        onClick = { repeatEnabled = false },
                        label = { Text("不重复") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = repeatEnabled,
                        onClick = { repeatEnabled = true },
                        label = { Text("重复") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
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
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("每")
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$interval",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = units.firstOrNull { it.first == selectedType }?.second ?: "",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val repeatType = if (repeatEnabled) selectedType else "none"
                    onUpdate(content, category, dueDate, repeatType, interval)
                },
                enabled = content.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun QuickAddSection(onAdd: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                "⚡ 快速添加",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAddChip(
                    icon = "💊",
                    label = "用药",
                    onClick = { onAdd("用药") },
                    modifier = Modifier.weight(1f)
                )
                QuickAddChip(
                    icon = "🏥",
                    label = "复查",
                    onClick = { onAdd("复查") },
                    modifier = Modifier.weight(1f)
                )
                QuickAddChip(
                    icon = "🔬",
                    label = "检查",
                    onClick = { onAdd("检查") },
                    modifier = Modifier.weight(1f)
                )
                QuickAddChip(
                    icon = "📋",
                    label = "其他",
                    onClick = { onAdd("其他") },
                    modifier = Modifier.weight(1f)
                )
            }
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
        color = Primary.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Primary
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                "📅 本周计划",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            if (weeklyTodos.isEmpty()) {
                Text(
                    "本周暂无待办",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                weeklyTodos.take(5).forEach { todo ->
                    val dayLabel = when (todo.dueDate) {
                        today -> "今天"
                        today.plusDays(1) -> "明天"
                        today.plusDays(2) -> "后天"
                        else -> {
                            val dayOfWeek = todo.dueDate.dayOfWeek
                            when (dayOfWeek) {
                                java.time.DayOfWeek.MONDAY -> "周一"
                                java.time.DayOfWeek.TUESDAY -> "周二"
                                java.time.DayOfWeek.WEDNESDAY -> "周三"
                                java.time.DayOfWeek.THURSDAY -> "周四"
                                java.time.DayOfWeek.FRIDAY -> "周五"
                                java.time.DayOfWeek.SATURDAY -> "周六"
                                java.time.DayOfWeek.SUNDAY -> "周日"
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
                            color = if (todo.dueDate == today) Primary else MaterialTheme.colorScheme.onSurface,
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
                        "还有 ${weeklyTodos.size - 5} 项...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}