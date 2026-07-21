package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yy.medtrace.data.repository.MemberRepository
import com.yy.medtrace.data.repository.TodoRepository
import com.yy.medtrace.viewmodel.HomeViewModelFactory
import com.yy.medtrace.data.model.FamilyMember
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
import com.yy.medtrace.Screen
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
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(memberRepository, todoRepository))
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTodoDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

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
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Settings, "设置", tint = Color.White, modifier = Modifier.size(22.dp))
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
            contentPadding = PaddingValues(bottom = 20.dp)
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
                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .clickable { navController.navigate("member_detail/${member.id}") },
                            shape = AppShapes.large,
                            colors = CardDefaults.cardColors(containerColor = bg),
                            elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MemberAvatar(
                                        member = member,
                                        modifier = Modifier.size(48.dp),
                                        fallbackBackground = content.copy(alpha = 0.18f),
                                        fallbackContent = content
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            member.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = content
                                        )
                                        Text(
                                            age?.let { "${member.relation} · ${it}岁" } ?: member.relation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = content.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                val tag = buildTag(member)
                                if (tag.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (tag.contains("偏高") || tag.contains("异常"))
                                            MemberColors.ElderMaleBg else content.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (tag.contains("偏高") || tag.contains("异常"))
                                                MaterialTheme.colorScheme.error else content,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { showAddDialog = true },
                            shape = AppShapes.large,
                            colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.outlineVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, "添加家人", tint = Primary, modifier = Modifier.size(20.dp))
                                }
                                Text("添加家人", color = Primary)
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
                SectionTitle("今日健康待办")
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Notifications, "今日健康提醒", tint = Primary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("按时提醒，别让健康溜走", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { showTodoDialog = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Add, "添加待办", tint = Primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (uiState.todos.isEmpty()) {
                            Text(
                                "今天暂无健康计划",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "点击下方开始添加",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                QuickTodoChip(
                                    modifier = Modifier.weight(1f),
                                    text = "添加服药提醒",
                                    onClick = {
                                        showTodoDialog = true
                                    }
                                )
                                QuickTodoChip(
                                    modifier = Modifier.weight(1f),
                                    text = "添加体检复查",
                                    onClick = {
                                        showTodoDialog = true
                                    }
                                )
                            }
                        } else {
                            uiState.todos.forEachIndexed { idx, todo ->
                                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                TodayTodoItem(
                                    text = todo.content,
                                    memberName = todo.memberName,
                                    done = todo.done,
                                    onToggle = {
                                        viewModel.toggleTodoDone(todo.id, !todo.done)
                                    },
                                    onDelete = {
                                        viewModel.deleteTodo(todo)
                                    }
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
                        title = "医疗记录",
                        desc = "AI扫描或手动录入",
                        onClick = { navController.navigate("add_record") }
                    )
                    FunctionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MedicalInformation,
                        title = "病历档案",
                        desc = "病史与用药归档",
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
                        title = "健康趋势",
                        desc = "长期指标追踪",
                        onClick = { navController.navigate("trends") }
                    )
                    FunctionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Settings,
                        title = "功能设置",
                        desc = "隐私、备份与偏好",
                        onClick = { navController.navigate(Screen.Settings.route) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        MemberEditDialog(
            member = null,
            onDismiss = { showAddDialog = false },
            onSave = { m ->
                viewModel.addMember(m)
                showAddDialog = false
            }
        )
    }

    if (showTodoDialog) {
        AddTodoDialog(
            members = uiState.members,
            onDismiss = { showTodoDialog = false },
            onSave = { memberId, memberName, content, dueDate ->
                viewModel.addTodo(memberId, memberName, content, dueDate)
                showTodoDialog = false
            }
        )
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
    memberName: String,
    done: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = done,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = Primary)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = memberName.firstOrNull()?.toString() ?: "我",
                style = MaterialTheme.typography.labelSmall,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTodoDialog(
    members: List<FamilyMember>,
    onDismiss: () -> Unit,
    onSave: (memberId: Long, memberName: String, content: String, dueDate: LocalDate) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedMemberId by remember { mutableStateOf<Long?>(members.firstOrNull()?.id) }
    val selectedMember = members.firstOrNull { it.id == selectedMemberId }
    var memberExpanded by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加健康待办") },
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
            }
        },
        confirmButton = {
            Button(
                enabled = content.isNotBlank() && selectedMemberId != null,
                onClick = {
                    val m = selectedMember ?: return@Button
                    onSave(m.id, m.name, content.trim(), dueDate)
                }
            ) { Text("保存") }
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
                        .background(Primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, title, tint = Primary, modifier = Modifier.size(18.dp))
                }
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.weight(1f))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.caption)
        }
    }
}
