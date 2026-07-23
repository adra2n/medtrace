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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.medtrace.data.AppDatabase
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RemindersScreen(
    database: AppDatabase,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var todos by remember { mutableStateOf<List<HealthTodo>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        val membersFlow = database.familyMemberDao().getAllMembers()
        val todosFlow = database.healthTodoDao().getAll()
        combine(membersFlow, todosFlow) { m, t -> m to t }
            .collect { (m, t) ->
                members = m
                todos = t.sortedWith(compareBy({ it.done }, { it.dueDate }))
            }
    }

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
                                    scope.launch { database.healthTodoDao().setDone(todo.id, done) }
                                },
                                onDelete = { todo ->
                                    scope.launch { database.healthTodoDao().delete(todo) }
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
            onSave = { memberId, memberName, content, dueDate ->
                scope.launch {
                    database.healthTodoDao().insert(
                        HealthTodo(
                            memberId = memberId,
                            memberName = memberName,
                            content = content,
                            dueDate = dueDate
                        )
                    )
                }
                showAddDialog = false
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
    onDelete: (HealthTodo) -> Unit
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
                    onDelete = { onDelete(todo) }
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
    onDelete: () -> Unit
) {
    val isOverdue = todo.dueDate.isBefore(LocalDate.now()) && !todo.done
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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
