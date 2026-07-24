package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.data.repository.RecordRepository
import com.yy.medtrace.data.model.CountResult
import com.yy.medtrace.ui.components.EmptyState
import com.yy.medtrace.ui.components.MemberAvatar
import com.yy.medtrace.ui.state.SelectedMemberHolder
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.MemberColors
import com.yy.medtrace.ui.theme.Primary
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.ui.theme.computeAge
import com.yy.medtrace.ui.theme.memberCardColors
import com.yy.medtrace.ui.components.MemberEditDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    database: AppDatabase,
    navController: NavController,
    recordRepository: RecordRepository
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var recordCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<FamilyMember?>(null) }
    var pendingDelete by remember { mutableStateOf<FamilyMember?>(null) }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var todos by remember { mutableStateOf<List<HealthTodo>>(emptyList()) }

    LaunchedEffect(Unit) {
        database.familyMemberDao().getAllMembers()
            .collectLatest { list -> members = list }
    }

    LaunchedEffect(Unit) {
        database.healthTodoDao().getAll()
            .collectLatest { list -> todos = list }
    }

    LaunchedEffect(members) {
        if (members.isEmpty()) {
            recordCounts = emptyMap()
            return@LaunchedEffect
        }
        val memberIds = members.map { it.id }
        val countResults = recordRepository.countByMembers(memberIds)
        recordCounts = countResults.associate { it.patientId to it.count }
    }

    val today = LocalDate.now()
    val totalMembers = members.size
    val pendingTodos = todos.count { !it.done }
    val completedTodos = todos.count { it.done }
    val overdueTodos = todos.count { !it.done && it.dueDate.isBefore(today) }
    val completionRate = if (todos.isNotEmpty()) (completedTodos * 100 / todos.size) else 0

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "家庭管理",
                actions = {
                    IconButton(onClick = {
                        editingMember = null
                        showDialog = true
                    }) {
                        Icon(Icons.Default.Add, "新增家庭成员", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            if (members.isEmpty()) {
                EmptyFamily()
            } else {
                // 家庭健康仪表盘
                HealthDashboard(
                    totalMembers = totalMembers,
                    pendingTodos = pendingTodos,
                    completionRate = completionRate,
                    overdueTodos = overdueTodos
                )

                // 成员头像网格
                Text(
                    "成员概览",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                MemberAvatarGrid(
                    members = members,
                    todos = todos,
                    selectedMemberId = selectedMemberId,
                    onSelectMember = { memberId ->
                        selectedMemberId = if (selectedMemberId == memberId) null else memberId
                    }
                )

                // 选中成员的详情卡片
                selectedMemberId?.let { memberId ->
                    val member = members.find { it.id == memberId }
                    val memberTodos = todos.filter { it.memberId == memberId && !it.done }
                    val recordCount = recordCounts[memberId] ?: 0
                    if (member != null) {
                        MemberDetailCard(
                            member = member,
                            pendingTodos = memberTodos,
                            recordCount = recordCount,
                            onEdit = {
                                editingMember = member
                                showDialog = true
                            },
                            onDelete = { pendingDelete = member },
                            onAddRecord = {
                                scope.launch { SelectedMemberHolder.select(member.id, database) }
                                navController.navigate("add_record")
                            },
                            onViewRecords = {
                                scope.launch { SelectedMemberHolder.select(member.id, database) }
                                navController.navigate("member_detail/${member.id}")
                            }
                        )
                    }
                }

                // 待办事项列表
                if (todos.any { !it.done }) {
                    PendingTodosSection(todos = todos.filter { !it.done })
                }

                // 导出按钮
                OutlinedButton(
                    onClick = {
                        android.widget.Toast.makeText(
                            context,
                            "全家健康简报导出功能开发中",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("导出全家健康简报")
                }
            }
        }
    }

    if (showDialog) {
        MemberEditDialog(
            member = editingMember,
            onDismiss = { showDialog = false },
            onSave = { m ->
                scope.launch {
                    if (editingMember == null) database.familyMemberDao().insert(m)
                    else database.familyMemberDao().update(m)
                }
                showDialog = false
            }
        )
    }

    pendingDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除家庭成员") },
            text = { Text("确定删除「${member.name}」？其医疗记录将归入「未归属」，仍可在记录页查看。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        database.medicalRecordDao().reassignToUnknown(member.id)
                        database.familyMemberDao().deleteById(member.id)
                        if (SelectedMemberHolder.selectedMemberId.value == member.id) {
                            val fallback = database.familyMemberDao().getDefaultMember()?.id
                                ?: database.familyMemberDao().getAllMembersList().firstOrNull()?.id
                            if (fallback != null) {
                                SelectedMemberHolder.select(fallback, database)
                            } else {
                                SelectedMemberHolder.selectedMemberId.value = null
                            }
                        }
                    }
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun HealthDashboard(
    totalMembers: Int,
    pendingTodos: Int,
    completionRate: Int,
    overdueTodos: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "家庭健康仪表盘",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    icon = "👥",
                    value = "$totalMembers",
                    label = "成员",
                    color = Primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = "📋",
                    value = "$pendingTodos",
                    label = "待办",
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = "✅",
                    value = "$completionRate%",
                    label = "完成",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = "⚠️",
                    value = "$overdueTodos",
                    label = "逾期",
                    color = if (overdueTodos > 0) MaterialTheme.colorScheme.error else Color(0xFF9E9E9E),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.medium,
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MemberAvatarGrid(
    members: List<FamilyMember>,
    todos: List<HealthTodo>,
    selectedMemberId: Long?,
    onSelectMember: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        members.forEach { member ->
            val memberTodos = todos.filter { it.memberId == member.id && !it.done }
            val overdueCount = memberTodos.count { it.dueDate.isBefore(LocalDate.now()) }
            val isSelected = selectedMemberId == member.id
            val (cardBg, cardContent) = memberCardColors(member.relation, member.gender)

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectMember(member.id) },
                shape = AppShapes.medium,
                color = if (isSelected) Primary.copy(alpha = 0.15f) else cardBg,
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Primary) else null
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MemberAvatar(
                        member = member,
                        size = 48.dp,
                        fallbackBackground = cardContent.copy(alpha = 0.18f),
                        fallbackContent = cardContent
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        member.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = CircleShape,
                        color = if (overdueCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ) {
                        Text(
                            "${memberTodos.size}待办",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (overdueCount > 0) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (overdueCount > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "$overdueCount 逾期",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberDetailCard(
    member: FamilyMember,
    pendingTodos: List<HealthTodo>,
    recordCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddRecord: () -> Unit,
    onViewRecords: () -> Unit
) {
    val healthTags = buildList {
        member.allergy.split(",").map { it.trim() }.filter { it.isNotBlank() }
            .forEach { add("过敏 · $it") }
        member.chronic.split(",").map { it.trim() }.filter { it.isNotBlank() }
            .forEach { add("慢性病 · $it") }
        member.medicationNote.split(",").map { it.trim() }.filter { it.isNotBlank() }
            .forEach { add("用药 · $it") }
    }

    val (cardBg, cardContent) = memberCardColors(member.relation, member.gender)
    val age = computeAge(member.birthday)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 头部：头像 + 基本信息
            Row(verticalAlignment = Alignment.CenterVertically) {
                MemberAvatar(
                    member = member,
                    size = 56.dp,
                    fallbackBackground = cardContent.copy(alpha = 0.18f),
                    fallbackContent = cardContent
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            member.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = cardContent,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (member.isDefault) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = AppShapes.small,
                                color = cardContent.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    "默认",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cardContent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    val sub = buildList {
                        if (member.relation.isNotBlank()) add(member.relation)
                        age?.let { add("${it}岁") }
                        if (member.gender.isNotBlank()) add(member.gender)
                        if (member.bloodType.isNotBlank()) add("${member.bloodType}型")
                    }.joinToString(" · ")
                    if (sub.isNotBlank()) {
                        Text(
                            sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = cardContent.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // 健康标签
            if (healthTags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    healthTags.forEach { tag ->
                        val abnormal = tag.contains("偏高") || tag.contains("异常")
                        Surface(
                            shape = AppShapes.small,
                            color = if (abnormal) MemberColors.ElderMaleBg else cardContent.copy(alpha = 0.16f)
                        ) {
                            Text(
                                tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (abnormal) MaterialTheme.colorScheme.error else cardContent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 统计信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$recordCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cardContent
                    )
                    Text(
                        "医疗记录",
                        style = MaterialTheme.typography.labelSmall,
                        color = cardContent.copy(alpha = 0.7f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${pendingTodos.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (pendingTodos.isNotEmpty()) Color(0xFFFF9800) else cardContent
                    )
                    Text(
                        "待办事项",
                        style = MaterialTheme.typography.labelSmall,
                        color = cardContent.copy(alpha = 0.7f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val overdueCount = pendingTodos.count { it.dueDate.isBefore(LocalDate.now()) }
                    Text(
                        "$overdueCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (overdueCount > 0) MaterialTheme.colorScheme.error else cardContent
                    )
                    Text(
                        "逾期",
                        style = MaterialTheme.typography.labelSmall,
                        color = cardContent.copy(alpha = 0.7f)
                    )
                }
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    shape = AppShapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = cardContent)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("编辑", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onAddRecord,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    shape = AppShapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = cardContent)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加记录", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onViewRecords,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    shape = AppShapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = cardContent)
                ) {
                    Text("查看病历", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            // 删除按钮
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                shape = AppShapes.small,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("删除成员", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PendingTodosSection(todos: List<HealthTodo>) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val dayAfterTomorrow = today.plusDays(2)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📋 待办事项",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF9800).copy(alpha = 0.1f)
                ) {
                    Text(
                        "${todos.size}项",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            val groupedTodos = todos.groupBy { todo ->
                when {
                    todo.dueDate.isBefore(today) -> "逾期"
                    todo.dueDate == today -> "今天"
                    todo.dueDate == tomorrow -> "明天"
                    todo.dueDate == dayAfterTomorrow -> "后天"
                    else -> todo.dueDate.format(DateTimeFormatter.ofPattern("M月d日"))
                }
            }

            groupedTodos.forEach { (dateLabel, dateTodos) ->
                val isOverdue = dateLabel == "逾期"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = AppShapes.small,
                        color = if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else Color(0xFFFF9800).copy(alpha = 0.1f)
                    ) {
                        Text(
                            dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else Color(0xFFFF9800),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        dateTodos.take(3).forEach { todo ->
                            Text(
                                "${todo.memberName} - ${todo.content}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (dateTodos.size > 3) {
                            Text(
                                "还有${dateTodos.size - 3}项...",
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

@Composable
private fun EmptyFamily() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = AppShapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.People,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        EmptyState(
            icon = Icons.Default.People,
            title = "还没有家庭成员",
            hint = "点击右上角按钮，添加你的第一位家人"
        )
    }
}

