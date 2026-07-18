package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.ui.components.MemberAvatar
import com.yy.chiyaole.ui.state.SelectedMemberHolder
import com.yy.chiyaole.ui.theme.AppShapes
import com.yy.chiyaole.ui.theme.GradientTopBar
import com.yy.chiyaole.ui.theme.MemberColors
import com.yy.chiyaole.ui.theme.SoftElevation
import com.yy.chiyaole.ui.theme.cardContainerColor
import com.yy.chiyaole.ui.theme.computeAge
import com.yy.chiyaole.ui.theme.memberCardColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val ALLERGY_PRESETS = listOf("青霉素", "头孢", "海鲜", "花粉", "鸡蛋", "牛奶")
private val CHRONIC_PRESETS = listOf("高血压", "糖尿病", "心脏病", "哮喘", "痛风", "慢性胃炎")
private val MEDICATION_PRESETS = listOf("餐前服", "餐后服", "忌酒", "定期复查")
private val BLOOD_PRESETS = listOf("A", "B", "AB", "O")
private val GENDER_OPTIONS = listOf("男", "女", "其他")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    database: AppDatabase,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var recordCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<FamilyMember?>(null) }
    var pendingDelete by remember { mutableStateOf<FamilyMember?>(null) }

    LaunchedEffect(Unit) {
        database.familyMemberDao().getAllMembers()
            .collectLatest { list -> members = list }
    }

    LaunchedEffect(members) {
        if (members.isEmpty()) {
            recordCounts = emptyMap()
            return@LaunchedEffect
        }
        val counts = members.associate { member ->
            member.id to database.medicalRecordDao().countByMember(member.id)
        }
        recordCounts = counts
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "全部家庭成员",
                actions = {
                    IconButton(onClick = {
                        editingMember = null
                        showDialog = true
                    }) {
                        Icon(Icons.Default.Add, "新增家庭成员")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            val context = LocalContext.current
            FloatingActionButton(
                onClick = {
                    android.widget.Toast.makeText(
                        context,
                        "全家健康简报导出功能开发中",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    "批量导出\n全家健康简报",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (members.isEmpty()) {
                EmptyFamily()
            } else {
                members.forEach { member ->
                    MemberProfileCard(
                        member = member,
                        recordCount = recordCounts[member.id] ?: 0,
                        onEdit = {
                            editingMember = member
                            showDialog = true
                        },
                        onDelete = { pendingDelete = member },
                        onOpenRecords = {
                            scope.launch { SelectedMemberHolder.select(member.id, database) }
                            navController.navigate("member_detail/${member.id}")
                        }
                    )
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
            text = { Text("确定删除「${member.name}」？其医疗记录将予以保留。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
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
        Text(
            "还没有家庭成员",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "点击右下角按钮，添加你的第一位家人",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberProfileCard(
    member: FamilyMember,
    recordCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenRecords: () -> Unit
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
        modifier = Modifier.fillMaxWidth(),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MemberAvatar(
                        member = member,
                        size = 48.dp,
                        fallbackBackground = cardContent.copy(alpha = 0.18f),
                        fallbackContent = cardContent
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                member.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = cardContent
                            )
                            if (member.isDefault) {
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
            }

            if (healthTags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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

            HorizontalDivider(color = cardContent.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenRecords,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("查看档案")
                }
                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("编辑")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberEditDialog(
    member: FamilyMember?,
    onDismiss: () -> Unit,
    onSave: (FamilyMember) -> Unit
) {
    var name by remember { mutableStateOf(member?.name ?: "") }
    var relation by remember { mutableStateOf(member?.relation ?: "") }
    var gender by remember { mutableStateOf(member?.gender ?: "") }
    var birthday by remember { mutableStateOf(member?.birthday ?: "") }
    var bloodType by remember { mutableStateOf(member?.bloodType ?: "") }
    var allergy by remember { mutableStateOf(member?.allergy ?: "") }
    var chronic by remember { mutableStateOf(member?.chronic ?: "") }
    var medicationNote by remember { mutableStateOf(member?.medicationNote ?: "") }
    var otherNote by remember { mutableStateOf(member?.otherNote ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (member == null) "新增家庭成员" else "编辑家庭成员") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("姓名 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(relation, { relation = it }, label = { Text("关系（如 本人/父亲/子女）") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                DialogSectionTitle("性别")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GENDER_OPTIONS.forEach { g ->
                        FilterChip(
                            selected = gender == g,
                            onClick = { gender = if (gender == g) "" else g },
                            label = { Text(g) }
                        )
                    }
                }

                val birthdayMillis = parseBirthdayMillis(birthday)
                var showDatePicker by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = birthday,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        label = { Text("生日") },
                        placeholder = { Text("点击选择") },
                        trailingIcon = {
                            Icon(Icons.Filled.Person, "选择生日")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = birthdayMillis ?: System.currentTimeMillis()
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    birthday = millisToBirthday(millis)
                                }
                                showDatePicker = false
                            }) { Text("确定") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("取消") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                DialogSectionTitle("血型与医疗信息")
                PresetField("血型", bloodType, { bloodType = it }, BLOOD_PRESETS, onPick = { bloodType = it })
                PresetField("过敏史", allergy, { allergy = it }, ALLERGY_PRESETS, onPick = { allergy = appendCsv(allergy, it) })
                PresetField("慢性病", chronic, { chronic = it }, CHRONIC_PRESETS, onPick = { chronic = appendCsv(chronic, it) })
                PresetField("用药注意", medicationNote, { medicationNote = it }, MEDICATION_PRESETS, onPick = { medicationNote = appendCsv(medicationNote, it) })
                OutlinedTextField(otherNote, { otherNote = it }, label = { Text("其他备注") }, singleLine = false, maxLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val result = FamilyMember(
                        id = member?.id ?: 0,
                        name = name.trim(),
                        relation = relation.trim(),
                        gender = gender,
                        birthday = birthday.trim(),
                        bloodType = bloodType.trim(),
                        allergy = allergy.trim(),
                        chronic = chronic.trim(),
                        medicationNote = medicationNote.trim(),
                        otherNote = otherNote.trim(),
                        isDefault = member?.isDefault ?: false
                    )
                    onSave(result)
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun DialogSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun PresetField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    presets: List<String>,
    onPick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { p ->
                FilterChip(
                    selected = false,
                    onClick = { onPick(p) },
                    label = { Text(p) }
                )
            }
        }
    }
}

private fun appendCsv(current: String, value: String): String {
    val parts = current.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
    parts.add(value)
    return parts.joinToString(",")
}

private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

private fun parseBirthdayMillis(value: String): Long? {
    if (value.isBlank()) return null
    return try {
        Calendar.getInstance().apply { time = DATE_FORMAT.parse(value)!! }.timeInMillis
    } catch (_: Exception) {
        null
    }
}

private fun millisToBirthday(millis: Long): String = DATE_FORMAT.format(Date(millis))
