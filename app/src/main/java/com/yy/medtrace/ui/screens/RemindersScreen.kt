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

    LaunchedEffect(Unit) {
        viewModel.loadReminders()
    }

    val members = uiState.members
    val todos = uiState.todos
    val filteredTodos = remember(todos, selectedMemberId) {
        if (selectedMemberId == null) todos
        else todos.filter { it.memberId == selectedMemberId }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            MemberFilterRow(
                members = members,
                selectedMemberId = selectedMemberId,
                onSelect = { selectedMemberId = it }
            )
            Spacer(Modifier.height(8.dp))
            if (filteredTodos.isEmpty()) {
                EmptyReminders(onAdd = { showAddDialog = true })
            } else {
                val grouped = filteredTodos.groupBy { it.memberId }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    grouped.forEach { (memberId, memberTodos) ->
                        val member = members.find { it.id == memberId }
                        item {
                            ReminderMemberGroup(
                                member = member,
                                todos = memberTodos,
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
                Text(
                    text = member?.name ?: "未知成员",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.weight(1f))
                val pending = todos.count { !it.done }
                if (pending > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "$pending 待办",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

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
    var selectedType by remember { mutableStateOf(todo.repeatType) }
    var interval by remember { mutableIntStateOf(todo.repeatInterval) }

    val presets = listOf(
        "none" to "不重复",
        "day" to "天",
        "week" to "周",
        "month" to "月",
        "year" to "年"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改重复设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = todo.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.forEach { (type, label) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                if (type != "none" && interval < 1) interval = 1
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (selectedType != "none") {
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
                            text = presets.firstOrNull { it.first == selectedType }?.second ?: "",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedType, interval) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
