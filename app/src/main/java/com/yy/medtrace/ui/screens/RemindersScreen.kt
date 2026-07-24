package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.ui.components.EmptyState
import com.yy.medtrace.ui.components.MemberAvatar
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.Primary
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.ui.theme.memberCardColors
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
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var editingTodo by remember { mutableStateOf<HealthTodo?>(null) }
    var calendarExpanded by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableIntStateOf(0) } // 0=全部, 1=待完成, 2=已完成
    var expandedMembers by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(Unit) {
        viewModel.loadReminders()
    }

    val members = uiState.members
    val todos = uiState.todos
    val filteredTodos = remember(todos, selectedMemberId, statusFilter) {
        var result = if (selectedMemberId == null) todos
        else todos.filter { it.memberId == selectedMemberId }
        when (statusFilter) {
            1 -> result = result.filter { !it.done }
            2 -> result = result.filter { it.done }
        }
        result
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "健康提醒",
                subtitle = "服药复查，不遗漏",
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                MedicationCalendarHeader(
                    todos = filteredTodos,
                    expanded = calendarExpanded,
                    onToggle = { calendarExpanded = !calendarExpanded }
                )
            }
            if (calendarExpanded) {
                item {
                    MedicationCalendar(todos = filteredTodos)
                }
            }
            item {
                StatusFilterRow(
                    statusFilter = statusFilter,
                    onSelect = { statusFilter = it }
                )
            }
            item {
                MemberFilterRow(
                    members = members,
                    selectedMemberId = selectedMemberId,
                    onSelect = { selectedMemberId = it }
                )
            }
            if (filteredTodos.isEmpty()) {
                item {
                    EmptyReminders(onAdd = { showAddDialog = true })
                }
            } else {
                val grouped = filteredTodos.groupBy { it.memberId }
                grouped.forEach { (memberId, memberTodos) ->
                    val member = members.find { it.id == memberId }
                    val isExpanded = expandedMembers.contains(memberId)
                    val pendingCount = memberTodos.count { !it.done }
                    item(key = "member_$memberId") {
                        ReminderMemberGroup(
                            member = member,
                            todos = memberTodos,
                            pendingCount = pendingCount,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedMembers = if (isExpanded) expandedMembers - memberId
                                else expandedMembers + memberId
                            },
                            onToggle = { todo, done ->
                                viewModel.setTodoDone(todo.id, done)
                            },
                            onDelete = { todo ->
                                viewModel.deleteTodo(todo)
                            },
                            onEditRepeat = { todo ->
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
            onDismiss = { showAddDialog = false },
            onSave = { memberId, memberName, content, dueDate, repeatType, repeatInterval ->
                viewModel.insertTodo(
                    HealthTodo(
                        memberId = memberId,
                        memberName = memberName,
                        content = content,
                        dueDate = dueDate,
                        repeatType = repeatType,
                        repeatInterval = repeatInterval
                    )
                )
                showAddDialog = false
            }
        )
    }

    editingTodo?.let { todo ->
        RepeatEditDialog(
            todo = todo,
            onDismiss = { editingTodo = null },
            onConfirm = { type, interval ->
                viewModel.updateRepeat(todo.id, type, interval)
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
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = AppShapes.large,
            color = Primary.copy(alpha = 0.12f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.AlarmOn,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Primary
                )
            }
        }
        EmptyState(
            icon = Icons.Default.Alarm,
            title = "暂无提醒",
            hint = "点击右上角添加服药、复查等提醒",
            action = {
                Button(onClick = onAdd) {
                    Text("添加提醒")
                }
            }
        )
    }
}

@Composable
private fun ReminderMemberGroup(
    member: FamilyMember?,
    todos: List<HealthTodo>,
    pendingCount: Int = todos.count { !it.done },
    isExpanded: Boolean = true,
    onToggleExpand: () -> Unit = {},
    onToggle: (HealthTodo, Boolean) -> Unit,
    onDelete: (HealthTodo) -> Unit,
    onEditRepeat: (HealthTodo) -> Unit
) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (member != null) {
                    val (bg, content) = memberCardColors(member.relation, member.gender)
                    MemberAvatar(
                        member = member,
                        size = 36.dp,
                        fallbackBackground = bg,
                        fallbackContent = content
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "?",
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member?.name ?: "未知成员",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${todos.size}项 · ${pendingCount}待办",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.Remove else Icons.Default.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded) {
                HorizontalDivider()

                todos.forEach { todo ->
                    ReminderTodoItem(
                        todo = todo,
                        onToggle = { onToggle(todo, !todo.done) },
                        onDelete = { onDelete(todo) },
                        onEditRepeat = { onEditRepeat(todo) }
                    )
                    if (todo != todos.last()) {
                        HorizontalDivider(modifier = Modifier.padding(start = 44.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderTodoItem(
    todo: HealthTodo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEditRepeat: () -> Unit
) {
    val isOverdue = todo.dueDate.isBefore(LocalDate.now()) && !todo.done
    val repeatLabel = com.yy.medtrace.viewmodel.RemindersViewModel.repeatLabel(todo.repeatType, todo.repeatInterval)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = todo.done,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = Primary)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todo.content,
                style = MaterialTheme.typography.bodyLarge,
                color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (todo.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = todo.dueDate.format(DateTimeFormatter.ofPattern("MM-dd")),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (repeatLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Primary.copy(alpha = 0.12f),
                        modifier = Modifier.clickable { onEditRepeat() }
                    ) {
                        Text(
                            text = repeatLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
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
private fun MemberFilterRow(
    members: List<FamilyMember>,
    selectedMemberId: Long?,
    onSelect: (Long?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedMemberId == null,
            onClick = { onSelect(null) },
            label = { Text("全部") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Primary,
                selectedLabelColor = Color.White
            )
        )
        members.forEach { member ->
            FilterChip(
                selected = selectedMemberId == member.id,
                onClick = { onSelect(member.id) },
                label = { Text(member.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary,
                    selectedLabelColor = Color.White
                )
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
                        IconButton(
                            onClick = { if (interval > 1) interval-- },
                            enabled = interval > 1
                        ) {
                            Icon(Icons.Default.Remove, "减少", tint = Primary)
                        }
                        Text(
                            text = "$interval",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        IconButton(
                            onClick = { if (interval < 99) interval++ }
                        ) {
                            Icon(Icons.Default.Add, "增加", tint = Primary)
                        }
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
@Composable
private fun MedicationCalendarHeader(
    todos: List<HealthTodo>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val completedCount = todos.count { it.done }
    val pendingCount = todos.count { !it.done }
    val today = LocalDate.now()
    val todayTodos = todos.filter { it.dueDate == today }
    val todayDone = todayTodos.count { it.done }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    today.format(DateTimeFormatter.ofPattern("M月")),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "今日 $todayDone/${todayTodos.size} · 总计 $completedCount/${todos.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                    Spacer(Modifier.width(3.dp))
                    Text("完成", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Primary.copy(alpha = 0.3f)))
                    Spacer(Modifier.width(3.dp))
                    Text("待服", style = MaterialTheme.typography.labelSmall)
                }
                Icon(
                    if (expanded) Icons.Default.Remove else Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusFilterRow(
    statusFilter: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("全部", "待完成", "已完成").forEachIndexed { index, label ->
            FilterChip(
                selected = statusFilter == index,
                onClick = { onSelect(index) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary.copy(alpha = 0.1f),
                    selectedLabelColor = Primary
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MedicationCalendar(
    todos: List<HealthTodo>
) {
    val today = LocalDate.now()
    val completedSet = remember(todos) { todos.filter { it.done }.map { it.dueDate.toString() }.toSet() }
    val pendingSet = remember(todos) { todos.filter { !it.done }.map { it.dueDate.toString() }.toSet() }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(today.format(DateTimeFormatter.ofPattern("M月")), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                        Spacer(Modifier.width(3.dp))
                        Text("完成", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Primary.copy(alpha = 0.3f)))
                        Spacer(Modifier.width(3.dp))
                        Text("待服", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日","一","二","三","四","五","六").forEach { d ->
                    Text(d, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            val days = (1..today.lengthOfMonth()).map { today.withDayOfMonth(it) }
            days.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        val done = date.toString() in completedSet
                        val pending = date.toString() in pendingSet
                        val isToday = date == today
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1.2f).padding(1.5.dp)
                                .clip(CircleShape)
                                .background(when { done -> Color(0xFF4CAF50); pending -> Primary.copy(alpha = 0.3f); isToday -> Primary.copy(alpha = 0.1f); else -> Color.Transparent }),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${date.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = when { done -> Color.White; isToday -> Primary; else -> MaterialTheme.colorScheme.onSurface })
                        }
                    }
                    repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}
