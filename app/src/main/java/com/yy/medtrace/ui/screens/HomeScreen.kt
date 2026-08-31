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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yy.medtrace.R
import androidx.navigation.NavController
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.data.settings.UserMode
import com.yy.medtrace.data.settings.UserModeStore
import com.yy.medtrace.ui.components.EmptyState
import com.yy.medtrace.ui.components.EmptyHomeState
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.ui.theme.computeAge
import com.yy.medtrace.ui.theme.memberCardColors
import com.yy.medtrace.ui.theme.caption
import com.yy.medtrace.ui.theme.Info
import com.yy.medtrace.ui.theme.Healthy
import com.yy.medtrace.ui.theme.Reminder
import com.yy.medtrace.ui.theme.NoStatus
import com.yy.medtrace.ui.theme.Urgent
import com.yy.medtrace.ui.components.MemberAvatar
import com.yy.medtrace.ui.components.MemberEditDialog
import com.yy.medtrace.viewmodel.HomeViewModel
import com.yy.medtrace.navigation.Screen
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddTodoDialog by remember { mutableStateOf(false) }
    var showAddRecordSheet by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val userModeStore = remember { UserModeStore(context) }
    val currentMode by userModeStore.currentMode.collectAsState()
    val isElderlyMode = currentMode == UserMode.ELDERLY

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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddRecordSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.screen_home_fab_add))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = if (isElderlyMode) 24.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isElderlyMode) 24.dp else 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle(
                        text = stringResource(R.string.screen_home_my_family),
                        fontSize = if (isElderlyMode) 20.sp else MaterialTheme.typography.titleMedium.fontSize
                    )
                    if (!isElderlyMode) {
                        TextButton(onClick = { navController.navigate("family") }) {
                            Text(stringResource(R.string.screen_home_family_manage))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val familyListState = rememberLazyListState()
                Box {
                    LazyRow(
                        state = familyListState,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(uiState.members) { member ->
                        val (bg, content) = memberCardColors(member.relation, member.gender)
                        val age = computeAge(member.birthday)
                        val memberTodoCount = uiState.todos.count { it.memberId == member.id && !it.done }
                        Card(
                            modifier = Modifier
                                .width(if (isElderlyMode) 200.dp else 180.dp)
                                .clickable { navController.navigate("member_detail/${member.id}") },
                            shape = AppShapes.large,
                            colors = CardDefaults.cardColors(containerColor = bg),
                            elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MemberAvatar(
                                        member = member,
                                        modifier = Modifier.size(if (isElderlyMode) 52.dp else 44.dp),
                                        fallbackBackground = content.copy(alpha = 0.18f),
                                        fallbackContent = content
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            member.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = if (isElderlyMode) 20.sp else MaterialTheme.typography.titleMedium.fontSize
                                            ),
                                            color = content,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                            Text(
                                            stringResource(R.string.screen_home_member_info, member.relation, age ?: 0),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = if (isElderlyMode) 16.sp else MaterialTheme.typography.bodySmall.fontSize
                                            ),
                                            color = content.copy(alpha = 0.8f),
                                            maxLines = 1
                                        )
                                    }
                                }
                                // 健康标签或待办数量（互斥显示）
                                val tag = buildTag(member)
                                if (tag.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = content.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            tag,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = if (isElderlyMode) 14.sp else MaterialTheme.typography.labelSmall.fontSize
                                            ),
                                            color = content.copy(alpha = 0.9f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else if (memberTodoCount > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "💊",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = if (isElderlyMode) 16.sp else MaterialTheme.typography.labelSmall.fontSize
                                            )
                                        )
                                        Text(
                                            stringResource(R.string.screen_home_todo_count, memberTodoCount),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = if (isElderlyMode) 14.sp else MaterialTheme.typography.labelSmall.fontSize
                                            ),
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
                SectionTitle(
                    text = stringResource(R.string.screen_home_today_reminders, pendingCount),
                    fontSize = if (isElderlyMode) 20.sp else MaterialTheme.typography.titleMedium.fontSize
                )
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.todos.isEmpty()) {
                            EmptyHomeState(
                                onAdd = { showAddTodoDialog = true }
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
                                    allTodos = uiState.todos,
                                    isElderlyMode = isElderlyMode
                                )
                            }
                            if (uiState.todos.size > 5) {
                                Text(
                                    stringResource(R.string.screen_home_view_all, uiState.pendingCount),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = if (isElderlyMode) 16.sp else MaterialTheme.typography.labelMedium.fontSize
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
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

            if (!isElderlyMode && uiState.recentRecords.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle(stringResource(R.string.screen_home_recent_records))
                        TextButton(onClick = {
                            navController.navigate("medical_records") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                            Text(stringResource(R.string.screen_home_view_all_records))
                        }
                    }
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
                                .padding(16.dp),
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

            if (!isElderlyMode) {
                item {
                    // 快捷功能标题
                    SectionTitle(stringResource(R.string.screen_home_quick_functions))
                    Spacer(Modifier.height(8.dp))
                    
                    // 2x2 功能网格
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FunctionTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.CameraAlt,
                                title = stringResource(R.string.screen_home_visit_record),
                                desc = stringResource(R.string.screen_home_visit_record_desc),
                                color = Info,
                                onClick = { showAddRecordSheet = true }
                            )
                            FunctionTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.MedicalInformation,
                                title = stringResource(R.string.screen_home_medical_archive),
                                desc = stringResource(R.string.screen_home_medical_archive_desc),
                                color = Healthy,
                                onClick = {
                                    navController.navigate("medical_records") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FunctionTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.InsertChart,
                                title = stringResource(R.string.screen_home_data_statistics),
                                desc = stringResource(R.string.screen_home_data_statistics_desc),
                                color = Reminder,
                                onClick = { navController.navigate("trends") }
                            )
                            FunctionTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Settings,
                                title = stringResource(R.string.screen_home_settings),
                                desc = stringResource(R.string.screen_home_settings_desc),
                                color = NoStatus,
                                onClick = { navController.navigate(Screen.Settings.route) }
                            )
                        }
                    }
                }
            }
        }
    }

    // 添加待办对话框
    if (showAddTodoDialog) {
        AddTodoDialog(
            members = uiState.members,
            onDismiss = { showAddTodoDialog = false },
            onSave = { memberId, memberName, content, dueDate, repeatType, category, reminderTime ->
                viewModel.addTodo(
                    memberId = memberId,
                    memberName = memberName,
                    content = content,
                    dueDate = dueDate,
                    repeatType = repeatType,
                    category = category,
                    reminderTime = reminderTime
                )
                showAddTodoDialog = false
            }
        )
    }

    // 添加就诊记录 BottomSheet
    if (showAddRecordSheet) {
        AddRecordBottomSheet(
            onDismiss = { showAddRecordSheet = false },
            onNext = { memberId, visitType, diagnosis, hospital, onsetTime ->
                showAddRecordSheet = false
                navController.navigate("add_record/-1?memberId=$memberId&diagnosis=$diagnosis&hospital=$hospital&onsetTime=$onsetTime&visitType=$visitType")
            },
            members = uiState.members
        )
    }
}

@Composable
private fun SectionTitle(text: String, fontSize: androidx.compose.ui.unit.TextUnit = MaterialTheme.typography.titleMedium.fontSize) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontSize = fontSize),
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun buildTag(member: FamilyMember): String {
    return when {
        member.chronic.isNotBlank() -> member.chronic
        member.allergy.isNotBlank() -> stringResource(R.string.screen_home_member_allergy, member.allergy)
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
    allTodos: List<HealthTodo> = emptyList(),
    isElderlyMode: Boolean = false
) {
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
        stringResource(R.string.screen_home_category_medication) -> "💊"
        stringResource(R.string.screen_home_category_review) -> "🏥"
        stringResource(R.string.screen_home_category_checkup) -> "🔬"
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
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            if (showCelebration) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Healthy,
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
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (isElderlyMode) 18.sp else MaterialTheme.typography.bodyMedium.fontSize
                ),
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (done) TextDecoration.LineThrough else null
            )
            if (todo != null) {
                val today = java.time.LocalDate.now()
                val categoryLabel = when (todo.category) {
                    stringResource(R.string.screen_home_category_medication) -> stringResource(R.string.screen_home_category_medication)
                    stringResource(R.string.screen_home_category_review) -> stringResource(R.string.screen_home_review_label)
                    stringResource(R.string.screen_home_category_checkup) -> stringResource(R.string.screen_home_checkup_label)
                    else -> stringResource(R.string.screen_home_category_other)
                }
                val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, todo.dueDate)
                val dateLabel = when {
                    daysUntil < 0 -> stringResource(R.string.screen_home_overdue, "", -daysUntil.toInt()).trim()
                    daysUntil == 0L -> stringResource(R.string.screen_home_today_label)
                    daysUntil <= 7 -> stringResource(R.string.screen_home_days_later, "", daysUntil.toInt()).trim()
                    else -> todo.dueDate.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            categoryLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (isElderlyMode) 14.sp else MaterialTheme.typography.labelSmall.fontSize
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        dateLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = if (isElderlyMode) 14.sp else MaterialTheme.typography.labelSmall.fontSize
                        ),
                        color = if (daysUntil < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

@Composable
private fun getTimeAgo(dateTime: java.time.LocalDateTime): String {
    val now = java.time.LocalDateTime.now()
    val minutes = java.time.Duration.between(dateTime, now).toMinutes()
    return when {
        minutes < 60 -> stringResource(R.string.screen_home_minutes_ago, minutes.toInt())
        minutes < 1440 -> stringResource(R.string.screen_home_hours_ago, (minutes / 60).toInt())
        minutes < 10080 -> stringResource(R.string.screen_home_days_ago, (minutes / 1440).toInt())
        else -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("M月d日"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddTodoDialog(
    members: List<FamilyMember>,
    initialCategory: String = stringResource(R.string.screen_home_category_other),
    onDismiss: () -> Unit,
    onSave: (memberId: Long, memberName: String, content: String, dueDate: LocalDate, repeatType: String, category: String, reminderTime: String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(initialCategory) }
    var selectedMemberId by remember { mutableStateOf<Long?>(members.firstOrNull()?.id) }
    val selectedMember = members.firstOrNull { it.id == selectedMemberId }
    var memberExpanded by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var repeatType by remember { mutableStateOf("none") }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf("09:00") }
    var showTimePicker by remember { mutableStateOf(false) }

    val categories = listOf(stringResource(R.string.screen_home_category_medication) to "💊", stringResource(R.string.screen_home_category_review) to "🏥", stringResource(R.string.screen_home_category_checkup) to "🔬", stringResource(R.string.screen_home_category_other) to "📋")

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
                ) { Text(stringResource(R.string.screen_home_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.screen_home_cancel)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showRepeatDialog) {
        RepeatPickerDialog(
            currentType = repeatType,
            onConfirm = { type ->
                repeatType = type
                showRepeatDialog = false
            },
            onDismiss = { showRepeatDialog = false }
        )
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_home_add_todo),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.screen_home_todo_content)) },
                placeholder = { Text(stringResource(R.string.screen_home_todo_content_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            // 提醒类别
            Text(stringResource(R.string.screen_home_reminder_category), style = MaterialTheme.typography.labelMedium)
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
            ExposedDropdownMenuBox(
                expanded = memberExpanded,
                onExpandedChange = { memberExpanded = !memberExpanded }
            ) {
                OutlinedTextField(
                    value = selectedMember?.let { stringResource(R.string.screen_home_member_format, it.name, it.relation.ifBlank { stringResource(R.string.screen_home_member_default) }) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text(stringResource(R.string.screen_home_related_member)) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, stringResource(R.string.screen_home_select_member), tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
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
                            text = { Text(stringResource(R.string.screen_home_member_format, m.name, m.relation.ifBlank { stringResource(R.string.screen_home_member_default) })) },
                            onClick = { selectedMemberId = m.id; memberExpanded = false }
                        )
                    }
                }
            }
            val dateLabel = if (dueDate == LocalDate.now()) stringResource(R.string.screen_home_today_label) else dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            OutlinedTextField(
                value = dateLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.screen_home_planned_date)) },
                trailingIcon = { Icon(Icons.Default.DateRange, stringResource(R.string.screen_home_select_date), tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                    disabledBorderColor = MaterialTheme.colorScheme.outline
                ),
                enabled = false
            )
            val repeatLabel = com.yy.medtrace.viewmodel.RemindersViewModel.repeatLabel(repeatType) ?: stringResource(R.string.screen_home_no_repeat)
            OutlinedTextField(
                value = repeatLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.screen_home_repeat)) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, stringResource(R.string.screen_home_select_repeat), tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth().clickable { showRepeatDialog = true },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                    disabledBorderColor = MaterialTheme.colorScheme.outline
                ),
                enabled = false
            )
            OutlinedTextField(
                value = reminderTime,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.screen_home_reminder_time)) },
                trailingIcon = { Icon(Icons.Default.DateRange, stringResource(R.string.screen_home_select_time), tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                    disabledBorderColor = MaterialTheme.colorScheme.outline
                ),
                enabled = false
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.screen_home_cancel)) }
                Button(
                    onClick = {
                        val m = selectedMember ?: return@Button
                        onSave(m.id, m.name, content.trim(), dueDate, repeatType, category, reminderTime)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = content.isNotBlank() && selectedMemberId != null
                ) { Text(stringResource(R.string.screen_home_save)) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RepeatPickerDialog(
    currentType: String,
    onConfirm: (type: String) -> Unit,
    onDismiss: () -> Unit
) {
    var repeatEnabled by remember { mutableStateOf(currentType != "none") }
    var selectedType by remember { mutableStateOf(if (currentType == "none") "day" else currentType) }

    val units = listOf("day" to stringResource(R.string.screen_home_unit_day), "week" to stringResource(R.string.screen_home_unit_week))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.screen_home_set_repeat)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = !repeatEnabled,
                        onClick = { repeatEnabled = false },
                        label = { Text(stringResource(R.string.screen_home_no_repeat)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = repeatEnabled,
                        onClick = { repeatEnabled = true },
                        label = { Text(stringResource(R.string.screen_home_repeat_enabled)) },
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
            Button(onClick = {
                val type = if (repeatEnabled) selectedType else "none"
                onConfirm(type)
            }) { Text(stringResource(R.string.screen_home_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.screen_home_cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddRecordBottomSheet(
    onDismiss: () -> Unit,
    onNext: (memberId: Long, visitType: String, diagnosis: String, hospital: String, onsetTime: String) -> Unit,
    members: List<FamilyMember>
) {
    var selectedMemberId by remember { mutableStateOf(members.firstOrNull()?.id ?: 0L) }
    var diagnosis by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var visitType by remember { mutableStateOf("门诊") }
    var customVisitType by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_add_record_title_add),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 成员选择
            if (members.isNotEmpty()) {
                Text(stringResource(R.string.screen_add_record_section_family_member), style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(members) { member ->
                        FilterChip(
                            selected = selectedMemberId == member.id,
                            onClick = { selectedMemberId = member.id },
                            label = { Text(member.name) }
                        )
                    }
                }
            }

            // 就诊类型
            Text(stringResource(R.string.screen_add_record_label_visit_type), style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val visitTypes = listOf("门诊", "急诊", "体检", "复查", "自购药", "其他")
                items(visitTypes) { type ->
                    FilterChip(
                        selected = visitType == type,
                        onClick = {
                            visitType = type
                            if (type != "其他") {
                                customVisitType = ""
                            }
                        },
                        label = { Text(type) }
                    )
                }
            }
            if (visitType == "其他") {
                OutlinedTextField(
                    value = customVisitType,
                    onValueChange = { customVisitType = it },
                    label = { Text(stringResource(R.string.screen_add_record_label_custom_visit_type)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // 病症记录（可选）
            OutlinedTextField(
                value = diagnosis,
                onValueChange = { diagnosis = it },
                label = { Text(stringResource(R.string.screen_add_record_label_diagnosis_detail)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 医院
            OutlinedTextField(
                value = hospital,
                onValueChange = { hospital = it },
                label = { Text(stringResource(R.string.screen_add_record_label_hospital)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 就诊时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.screen_add_record_label_visit_time, selectedDate.toString()))
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                }
            }

            // 下一步按钮
            Button(
                onClick = {
                    val onsetTime = "${selectedDate}T${selectedTime}"
                    val finalVisitType = if (visitType == "其他" && customVisitType.isNotBlank()) customVisitType else visitType
                    onNext(selectedMemberId, finalVisitType, diagnosis, hospital, onsetTime)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = visitType.isNotBlank()
            ) {
                Text(stringResource(R.string.screen_add_record_btn_next))
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // DatePicker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * 86400000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = LocalDate.ofEpochDay(millis / 86400000L)
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.screen_home_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.screen_home_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // TimePicker
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.screen_home_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.screen_home_cancel)) }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun FunctionTile(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    desc: String,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() }.height(100.dp),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, title, tint = color, modifier = Modifier.size(20.dp))
                }
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.weight(1f))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
