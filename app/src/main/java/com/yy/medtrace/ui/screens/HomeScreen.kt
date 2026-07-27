package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yy.medtrace.data.repository.MemberRepository
import com.yy.medtrace.data.repository.RecordRepository
import com.yy.medtrace.data.repository.TodoRepository
import com.yy.medtrace.viewmodel.HomeViewModelFactory
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.ui.components.EmptyState
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.MemberColors
import com.yy.medtrace.ui.theme.Primary
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.ui.theme.computeAge
import com.yy.medtrace.ui.theme.memberCardColors
import com.yy.medtrace.ui.theme.caption
import com.yy.medtrace.ui.components.MemberAvatar
import com.yy.medtrace.ui.components.MemberEditDialog
import com.yy.medtrace.viewmodel.HomeViewModel
import com.yy.medtrace.navigation.Screen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    memberRepository: MemberRepository,
    todoRepository: TodoRepository,
    recordRepository: RecordRepository,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(memberRepository, todoRepository, recordRepository))
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "医迹",
                subtitle = uiState.todayLabel,
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                SectionTitle("我的家人")
                Spacer(Modifier.height(12.dp))
                val familyListState = rememberLazyListState()
                Box {
                    LazyRow(
                        state = familyListState,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(uiState.members) { member ->
                        val (bg, content) = memberCardColors(member.relation, member.gender)
                        val age = computeAge(member.birthday)
                        val memberTodoCount = uiState.todos.count { it.memberId == member.id && !it.done }
                        Card(
                            modifier = Modifier
                                .width(180.dp)
                                .clickable { navController.navigate("member_detail/${member.id}") },
                            shape = AppShapes.large,
                            colors = CardDefaults.cardColors(containerColor = bg),
                            elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MemberAvatar(
                                        member = member,
                                        modifier = Modifier.size(44.dp),
                                        fallbackBackground = content.copy(alpha = 0.18f),
                                        fallbackContent = content
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            member.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = content,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            age?.let { "${member.relation} · ${it}岁" } ?: member.relation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = content.copy(alpha = 0.8f),
                                            maxLines = 1
                                        )
                                    }
                                }
                                // 健康状态指示
                                val tag = buildTag(member)
                                if (tag.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = content.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = content.copy(alpha = 0.9f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                // 待办统计
                                if (memberTodoCount > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "💊",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            "${memberTodoCount}项待办",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = content.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                    if (familyListState.canScrollForward) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(12.dp)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                                    )
                                )
                        )
                    }
                }
            }

            item {
                val pendingCount = uiState.todos.count { !it.done }
                SectionTitle("今日提醒 (${pendingCount}项待办)")
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.large,
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                    elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.todos.isEmpty()) {
                            EmptyState(
                                icon = Icons.Default.EventNote,
                                title = "今天暂无日程提醒",
                                hint = "点击添加用药、复查提醒"
                            )
                        } else {
                            uiState.todos.take(5).forEachIndexed { idx, todo ->
                                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                val todoMember = uiState.members.find { it.id == todo.memberId }
                                TodayTodoItem(
                                    text = todo.content,
                                    member = todoMember,
                                    done = todo.done,
                                    onToggle = { viewModel.toggleTodoDone(todo.id, !todo.done) },
                                    todo = todo,
                                    allTodos = uiState.todos
                                )
                            }
                            if (uiState.todos.size > 5) {
                                Text(
                                    "查看全部 ${uiState.pendingCount} 项待办",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navController.navigate(Screen.Reminders.route) }
                                        .padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.recentRecords.isNotEmpty()) {
                item {
                    SectionTitle("最近记录")
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.large,
                        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.recentRecords.forEach { record ->
                                RecentRecordItem(
                                    record = record,
                                    members = uiState.members,
                                    onClick = { navController.navigate("medical_records") }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle("快捷功能")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FunctionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CameraAlt,
                        title = "就诊记录",
                        desc = "AI扫描或手动录入",
                        color = Color(0xFF2196F3),
                        onClick = { navController.navigate("add_record") }
                    )
                    FunctionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MedicalInformation,
                        title = "病历档案",
                        desc = "病史与用药归档",
                        color = Color(0xFF4CAF50),
                        onClick = { navController.navigate("medical_records") }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FunctionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.InsertChart,
                        title = "数据统计",
                        desc = "长期指标追踪",
                        color = Color(0xFFFF9800),
                        onClick = { navController.navigate("trends") }
                    )
                    FunctionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Settings,
                        title = "功能设置",
                        desc = "隐私、备份与偏好",
                        color = Color(0xFF9E9E9E),
                        onClick = { navController.navigate(Screen.Settings.route) }
                    )
                }
            }
        }
    }

}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun QuickTodoChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() },
        shape = AppShapes.medium,
        color = Primary,
        shadowElevation = SoftElevation
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

private fun buildTag(member: FamilyMember): String {
    return when {
        member.chronic.isNotBlank() -> member.chronic
        member.allergy.isNotBlank() -> "过敏：${member.allergy}"
        member.medicationNote.isNotBlank() -> member.medicationNote
        else -> ""
    }
}

@Composable
private fun TodayTodoItem(
    text: String,
    member: FamilyMember?,
    done: Boolean,
    onToggle: () -> Unit,
    todo: HealthTodo? = null,
    allTodos: List<HealthTodo> = emptyList()
) {
    val streak = todo?.getStreak() ?: 0
    val progress = todo?.getProgress() ?: 0f
    var showCelebration by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (showCelebration) 1.2f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "scale"
    )
    
    LaunchedEffect(done) {
        if (done && todo != null) {
            showCelebration = true
            kotlinx.coroutines.delay(500)
            showCelebration = false
        }
    }
    
    // 类别图标
    val categoryIcon = when (todo?.category) {
        "用药" -> "💊"
        "复查" -> "🏥"
        "检查" -> "🔬"
        else -> "📋"
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.scale(scale)
        ) {
            Checkbox(
                checked = done,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = Primary)
            )
            if (showCelebration) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }
        }
        // 类别图标
        Text(
            categoryIcon,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.width(6.dp))
        if (member != null) {
            val (bg, content) = memberCardColors(member.relation, member.gender)
            MemberAvatar(
                member = member,
                size = 24.dp,
                fallbackBackground = bg,
                fallbackContent = content
            )
        }
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (done) TextDecoration.LineThrough else null
            )
            // 按类别显示不同统计
            if (todo != null) {
                val today = java.time.LocalDate.now()
                when (todo.category) {
                    "用药" -> {
                        if (streak > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6B35),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "连续 $streak 天",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFF6B35)
                                )
                            }
                        }
                    }
                    "复查", "检查" -> {
                        val label = if (todo.category == "复查") "复查" else "体检"
                        // 查找未来日期的待办
                        val futureTodo = allTodos.filter {
                            it.category == todo.category && !it.done && it.dueDate.isAfter(today)
                        }.minByOrNull { it.dueDate }
                        
                        val displayDate = futureTodo?.dueDate ?: todo.dueDate
                        val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, displayDate)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = if (daysUntil < 0) MaterialTheme.colorScheme.error else Primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                when {
                                    daysUntil < 0 -> "$label 已逾期${-daysUntil}天"
                                    daysUntil == 0L -> "$label 今天"
                                    daysUntil <= 7 -> "$label ${daysUntil}天后"
                                    else -> "$label ${displayDate.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (daysUntil < 0) MaterialTheme.colorScheme.error else Primary
                            )
                        }
                    }
                    else -> {
                        // 其他类别显示疗程进度
                        if (todo.durationDays > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Primary,
                                    trackColor = Primary.copy(alpha = 0.12f)
                                )
                                Text(
                                    "${todo.durationDays}天疗程",
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
}

@Composable
private fun RecentRecordItem(
    record: MedicalRecord,
    members: List<FamilyMember>,
    onClick: () -> Unit
) {
    val member = members.find { it.id == record.patientId }
    val timeAgo = getTimeAgo(record.onsetTime)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
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
                    .background(MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MedicalInformation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                record.diagnosis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    timeAgo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (member != null) {
                    Text(
                        "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        member.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getTimeAgo(dateTime: java.time.LocalDateTime): String {
    val now = java.time.LocalDateTime.now()
    val minutes = java.time.Duration.between(dateTime, now).toMinutes()
    return when {
        minutes < 60 -> "${minutes}分钟前"
        minutes < 1440 -> "${minutes / 60}小时前"
        minutes < 10080 -> "${minutes / 1440}天前"
        else -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("M月d日"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddTodoDialog(
    members: List<FamilyMember>,
    onDismiss: () -> Unit,
    onSave: (memberId: Long, memberName: String, content: String, dueDate: LocalDate, repeatType: String, repeatInterval: Int, category: String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("其他") }
    var selectedMemberId by remember { mutableStateOf<Long?>(members.firstOrNull()?.id) }
    val selectedMember = members.firstOrNull { it.id == selectedMemberId }
    var memberExpanded by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var repeatType by remember { mutableStateOf("none") }
    var repeatInterval by remember { mutableIntStateOf(1) }
    var showRepeatDialog by remember { mutableStateOf(false) }

    val categories = listOf("用药" to "💊", "复查" to "🏥", "检查" to "🔬", "其他" to "📋")

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
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

    if (showRepeatDialog) {
        RepeatPickerDialog(
            currentType = repeatType,
            currentInterval = repeatInterval,
            onConfirm = { type, interval ->
                repeatType = type
                repeatInterval = interval
                showRepeatDialog = false
            },
            onDismiss = { showRepeatDialog = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加待办提醒") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("待办内容") },
                    placeholder = { Text("如：晚8点服用降脂药") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                ExposedDropdownMenuBox(
                    expanded = memberExpanded,
                    onExpandedChange = { memberExpanded = !memberExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedMember?.let { "${it.name}（${it.relation.ifBlank { "成员" }}）" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("关联成员") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, "选择成员", tint = Primary) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = Primary,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    DropdownMenu(
                        expanded = memberExpanded,
                        onDismissRequest = { memberExpanded = false },
                        modifier = Modifier.exposedDropdownSize()
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.name}（${m.relation.ifBlank { "成员" }}）") },
                                onClick = { selectedMemberId = m.id; memberExpanded = false }
                            )
                        }
                    }
                }
                val dateLabel = if (dueDate == LocalDate.now()) "今天" else dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("计划日期") },
                    trailingIcon = { Icon(Icons.Default.DateRange, "选择日期", tint = Primary) },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = Primary,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    enabled = false
                )
                val repeatLabel = com.yy.medtrace.viewmodel.RemindersViewModel.repeatLabel(repeatType, repeatInterval) ?: "不重复"
                OutlinedTextField(
                    value = repeatLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("重复") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "选择重复", tint = Primary) },
                    modifier = Modifier.fillMaxWidth().clickable { showRepeatDialog = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = Primary,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    enabled = false
                )
            }
        },
        confirmButton = {
            Button(
                enabled = content.isNotBlank() && selectedMemberId != null,
                onClick = {
                    val m = selectedMember ?: return@Button
                    onSave(m.id, m.name, content.trim(), dueDate, repeatType, repeatInterval, category)
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun RepeatPickerDialog(
    currentType: String,
    currentInterval: Int,
    onConfirm: (type: String, interval: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var repeatEnabled by remember { mutableStateOf(currentType != "none") }
    var selectedType by remember { mutableStateOf(if (currentType == "none") "day" else currentType) }
    var interval by remember { mutableIntStateOf(if (currentInterval < 1) 1 else currentInterval) }

    val units = listOf("day" to "天", "week" to "周", "month" to "月", "year" to "年")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置重复") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

@Composable
private fun FunctionTile(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    desc: String,
    color: Color = Primary,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() }.height(88.dp),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, title, tint = color, modifier = Modifier.size(18.dp))
                }
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.weight(1f))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.caption)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun FamilyHealthOverview(
    members: List<FamilyMember>,
    todos: List<HealthTodo>
) {
    val today = LocalDate.now()
    val todayTodos = todos.filter { it.dueDate == today }
    val todayTotal = todayTodos.size
    val todayCompleted = todayTodos.count { it.done }
    val todayPending = todayTodos.count { !it.done }
    val todayOverdue = todayTodos.count { !it.done && it.dueDate.isBefore(today) }
    val progress = if (todayTotal > 0) todayCompleted.toFloat() / todayTotal else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TodayStatItem(
                    label = "今日待办",
                    value = "$todayTotal",
                    color = Primary
                )
                TodayStatItem(
                    label = "已完成",
                    value = "$todayCompleted",
                    color = Color(0xFF4CAF50)
                )
                TodayStatItem(
                    label = "逾期",
                    value = "$todayOverdue",
                    color = if (todayOverdue > 0) MaterialTheme.colorScheme.error else Color(0xFF9E9E9E)
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progress >= 0.8f) Color(0xFF4CAF50) else Primary,
                trackColor = Primary.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
private fun TodayStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
