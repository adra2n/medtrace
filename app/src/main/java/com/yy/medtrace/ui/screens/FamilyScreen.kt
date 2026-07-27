package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.repository.RecordRepository
import com.yy.medtrace.ui.components.EmptyState
import com.yy.medtrace.ui.components.MemberAvatar
import com.yy.medtrace.ui.state.SelectedMemberHolder
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.HealthTagColors
import com.yy.medtrace.ui.theme.Primary
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.ui.theme.computeAge
import com.yy.medtrace.ui.theme.memberCardColors
import com.yy.medtrace.ui.components.MemberEditDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    database: AppDatabase,
    navController: NavController,
    recordRepository: RecordRepository,
    premiumManager: com.yy.medtrace.data.settings.PremiumManager? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var recordCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var recentRecordsMap by remember { mutableStateOf<Map<Long, List<com.yy.medtrace.data.model.MedicalRecord>>>(emptyMap()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<FamilyMember?>(null) }
    var pendingDelete by remember { mutableStateOf<FamilyMember?>(null) }
    var expandedMemberId by remember { mutableStateOf<Long?>(null) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        database.familyMemberDao().getAllMembers()
            .collectLatest { list -> members = list }
    }

    LaunchedEffect(members) {
        if (members.isEmpty()) {
            recordCounts = emptyMap()
            recentRecordsMap = emptyMap()
            return@LaunchedEffect
        }
        val memberIds = members.map { it.id }
        val countResults = recordRepository.countByMembers(memberIds)
        recordCounts = countResults.associate { it.patientId to it.count }
        val recentMap = mutableMapOf<Long, List<com.yy.medtrace.data.model.MedicalRecord>>()
        members.forEach { member ->
            recordRepository.getRecentRecordsByMember(member.id, 2).collect { records ->
                recentMap[member.id] = records
            }
        }
        recentRecordsMap = recentMap
    }

    val totalMembers = members.size
    val recentRecordCount = recordCounts.values.sum()

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "家庭管理",
                subtitle = "家人档案一目了然",
                actions = {
                    IconButton(onClick = {
                        // 检查是否可以添加更多成员
                        if (premiumManager != null && !premiumManager.isPremiumActive() && members.size >= 1) {
                            showPremiumDialog = true
                        } else {
                            editingMember = null
                            showDialog = true
                        }
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
                // 家庭健康概览
                HealthDashboard(
                    members = members,
                    recentRecordCount = recentRecordCount
                )

                // 成员卡片列表
                Text(
                    "成员列表",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                members.forEach { member ->
                    val recordCount = recordCounts[member.id] ?: 0
                    val recentRecords = recentRecordsMap[member.id] ?: emptyList()
                    MemberCard(
                        member = member,
                        isExpanded = expandedMemberId == member.id,
                        recordCount = recordCount,
                        recentRecords = recentRecords,
                        onToggleExpand = {
                            expandedMemberId = if (expandedMemberId == member.id) null else member.id
                        },
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

    // 高级版提示对话框
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = { Text("解锁高级版") },
            text = { Text("免费版最多支持1位家庭成员。升级高级版可添加更多成员，还有更多功能等你解锁！") },
            confirmButton = {
                TextButton(onClick = {
                    showPremiumDialog = false
                    navController.navigate("premium")
                }) {
                    Text("立即升级")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumDialog = false }) {
                    Text("稍后再说")
                }
            }
        )
    }

    pendingDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除家庭成员") },
            text = { Text("确定删除「${member.name}」？其就诊记录将归入「未归属」，仍可在记录页查看。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        database.medicalRecordDao().reassignToUnknown(member.id)
                        database.familyMemberDao().deleteById(member.id)
                        if (com.yy.medtrace.ui.state.SelectedMemberHolder.selectedMemberId.value == member.id) {
                            val fallback = database.familyMemberDao().getDefaultMember()?.id
                                ?: database.familyMemberDao().getAllMembersList().firstOrNull()?.id
                            if (fallback != null) {
                                com.yy.medtrace.ui.state.SelectedMemberHolder.select(fallback, database)
                            } else {
                                com.yy.medtrace.ui.state.SelectedMemberHolder.selectedMemberId.value = null
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
            hint = "点击右上角按钮，添加你的第一位家人"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberCard(
    member: FamilyMember,
    isExpanded: Boolean,
    recordCount: Int,
    recentRecords: List<com.yy.medtrace.data.model.MedicalRecord>,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddRecord: () -> Unit,
    onViewRecords: () -> Unit
) {
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 头部：头像 + 基本信息 + 展开按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                MemberAvatar(
                    member = member,
                    size = if (isExpanded) 48.dp else 36.dp,
                    fallbackBackground = cardContent.copy(alpha = 0.18f),
                    fallbackContent = cardContent
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // 姓名 + 默认标签
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            member.name,
                            style = if (isExpanded) MaterialTheme.typography.titleMedium
                                   else MaterialTheme.typography.bodyMedium,
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
                    // 关系 · 年龄（收起状态）
                    if (!isExpanded) {
                        val sub = buildList {
                            if (member.relation.isNotBlank()) add(member.relation)
                            age?.let { add("${it}岁") }
                        }.joinToString(" · ")
                        if (sub.isNotBlank()) {
                            Text(
                                sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = cardContent.copy(alpha = 0.8f)
                            )
                        }
                    }
                    // 关系 · 年龄 · 性别 · 血型（展开状态）
                    if (isExpanded) {
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
                // 展开/收起按钮
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess
                                     else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = cardContent.copy(alpha = 0.6f)
                    )
                }
            }

            // 健康标签（收起状态）
            if (!isExpanded) {
                val healthTags = buildList {
                    member.allergy.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        .forEach { add("过敏: $it") }
                    member.chronic.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        .forEach { add(it) }
                }
                if (healthTags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        healthTags.forEach { tag ->
                            val isAllergy = tag.startsWith("过敏:")
                            Surface(
                                shape = AppShapes.small,
                                color = if (isAllergy) HealthTagColors.AllergyBg
                                       else HealthTagColors.ChronicBg
                            ) {
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAllergy) HealthTagColors.AllergyContent
                                           else HealthTagColors.ChronicContent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 展开状态的内容
            if (isExpanded) {
                // 健康详情表格
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (member.allergy.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "过敏",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = cardContent,
                                modifier = Modifier.width(56.dp)
                            )
                            Text(
                                member.allergy,
                                style = MaterialTheme.typography.bodySmall,
                                color = cardContent.copy(alpha = 0.8f)
                            )
                        }
                    }
                    if (member.chronic.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "慢性病",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = cardContent,
                                modifier = Modifier.width(56.dp)
                            )
                            Text(
                                member.chronic,
                                style = MaterialTheme.typography.bodySmall,
                                color = cardContent.copy(alpha = 0.8f)
                            )
                        }
                    }
                    if (member.medicationNote.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "用药",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = cardContent,
                                modifier = Modifier.width(56.dp)
                            )
                            Text(
                                member.medicationNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = cardContent.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // 最近就诊记录
                if (recentRecords.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "最近就诊",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = cardContent
                        )
                        recentRecords.take(2).forEach { record ->
                            Text(
                                "${record.onsetTime.format(DateTimeFormatter.ofPattern("MM-dd"))} ${record.diagnosis}",
                                style = MaterialTheme.typography.labelSmall,
                                color = cardContent.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 统计信息 + 操作按钮（一行）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "记录 $recordCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = cardContent.copy(alpha = 0.7f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(16.dp), tint = cardContent)
                        }
                        IconButton(
                            onClick = onAddRecord,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "添加记录", modifier = Modifier.size(16.dp), tint = cardContent)
                        }
                        IconButton(
                            onClick = onViewRecords,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "查看病历", modifier = Modifier.size(16.dp), tint = cardContent)
                        }
                    }
                }

                // 删除按钮
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("删除成员", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun HealthDashboard(
    members: List<FamilyMember>,
    recentRecordCount: Int
) {
    val chronicCount = members.count { it.chronic.isNotBlank() }
    val allergyCount = members.count { it.allergy.isNotBlank() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "家庭健康概览",
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
                    value = "${members.size}",
                    label = "成员",
                    color = Primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = "🏥",
                    value = "$chronicCount",
                    label = "慢性病",
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = "⚠️",
                    value = "$allergyCount",
                    label = "过敏",
                    color = if (allergyCount > 0) MaterialTheme.colorScheme.error else Color(0xFF9E9E9E),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = "📋",
                    value = "$recentRecordCount",
                    label = "本月记录",
                    color = Color(0xFF4CAF50),
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
