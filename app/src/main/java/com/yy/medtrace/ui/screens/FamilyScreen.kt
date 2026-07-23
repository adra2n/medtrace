package com.yy.medtrace.ui.screens

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
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.repository.RecordRepository
import com.yy.medtrace.data.model.CountResult
import com.yy.medtrace.ui.components.EmptyState
import com.yy.medtrace.ui.components.MemberAvatar
import com.yy.medtrace.ui.state.SelectedMemberHolder
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.MemberColors
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.ui.theme.computeAge
import com.yy.medtrace.ui.theme.memberCardColors
import com.yy.medtrace.ui.components.MemberEditDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    LaunchedEffect(Unit) {
        database.familyMemberDao().getAllMembers()
            .collectLatest { list -> members = list }
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 80.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        onClick = {
                            scope.launch { SelectedMemberHolder.select(member.id, database) }
                            navController.navigate("member_detail/${member.id}")
                        }
                    )
                }
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
                        .padding(top = 8.dp),
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
            hint = "点击右下角按钮，添加你的第一位家人"
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
    onClick: () -> Unit
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
            .clickable { onClick() },
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MemberAvatar(
                        member = member,
                        size = 44.dp,
                        fallbackBackground = cardContent.copy(alpha = 0.18f),
                        fallbackContent = cardContent
                    )
                    Spacer(Modifier.width(10.dp))
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
            }

            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onEdit() },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    shape = AppShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardContent,
                        contentColor = cardBg
                    )
                ) {
                    Text("编辑", style = MaterialTheme.typography.labelMedium)
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

