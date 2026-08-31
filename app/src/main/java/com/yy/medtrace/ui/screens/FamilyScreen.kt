package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.yy.medtrace.R
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.ui.components.EmptyState
import com.yy.medtrace.ui.components.MemberAvatar
import com.yy.medtrace.ui.state.SelectedMemberHolder
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.healthTagColorSets
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.ui.theme.computeAge
import com.yy.medtrace.ui.theme.memberCardColors
import com.yy.medtrace.ui.theme.Reminder
import com.yy.medtrace.ui.theme.NoStatus
import com.yy.medtrace.ui.theme.Healthy
import com.yy.medtrace.ui.components.MemberEditDialog
import com.yy.medtrace.viewmodel.FamilyViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    viewModel: FamilyViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val members = uiState.members
    val recordCounts = uiState.recordCounts
    val recentRecordsMap = uiState.recentRecordsMap
    var showDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<FamilyMember?>(null) }
    var pendingDelete by remember { mutableStateOf<FamilyMember?>(null) }
    var expandedMemberId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadMembers()
    }

    val totalMembers = members.size
    val recentRecordCount = recordCounts.values.sum()

    Scaffold(
        topBar = {
            GradientTopBar(
                title = stringResource(R.string.screen_family_title),
                subtitle = stringResource(R.string.screen_family_subtitle),
                actions = {
                    IconButton(onClick = {
                        editingMember = null
                        showDialog = true
                    }) {
                        Icon(Icons.Default.Add, stringResource(R.string.screen_family_add_member_content_desc), tint = MaterialTheme.colorScheme.primary)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            if (members.isEmpty()) {
                item { EmptyFamily() }
            } else {
                items(members, key = { it.id }) { member ->
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
                            scope.launch { SelectedMemberHolder.select(member.id, viewModel.database) }
                            navController.navigate("add_record/-1?memberId=${member.id}")
                        },
                        onViewRecords = {
                            scope.launch { SelectedMemberHolder.select(member.id, viewModel.database) }
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
                if (editingMember == null) viewModel.addMember(m)
                else viewModel.updateMember(m)
                showDialog = false
            }
        )
    }

    pendingDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.screen_family_delete_member_title)) },
            text = { Text(stringResource(R.string.screen_family_delete_member_message, member.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMember(member.id)
                    if (SelectedMemberHolder.selectedMemberId.value == member.id) {
                        scope.launch { SelectedMemberHolder.select(0L, viewModel.database) }
                    }
                    pendingDelete = null
                }) { Text(stringResource(R.string.screen_family_delete_button)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.screen_family_cancel_button)) }
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
            title = stringResource(R.string.screen_family_empty_title),
            hint = stringResource(R.string.screen_family_empty_hint)
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
            modifier = Modifier.fillMaxWidth()
        ) {
            // 头部：头像 + 姓名 + 关系/年龄（可点击展开）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemberAvatar(
                    member = member,
                    size = 40.dp,
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
                            fontWeight = FontWeight.Bold
                        )
                        if (member.isDefault) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = AppShapes.small,
                                color = cardContent.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    stringResource(R.string.screen_family_default_tag),
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
                    }.joinToString(" · ")
                    if (sub.isNotBlank()) {
                        Text(
                            sub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cardContent.copy(alpha = 0.7f)
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess
                                 else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = cardContent.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // 展开内容
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = cardContent.copy(alpha = 0.1f), thickness = 0.5.dp)
                    Spacer(Modifier.height(4.dp))

                    // 健康信息
                    if (member.allergy.isNotBlank()) {
                        Row {
                            Text(
                                "过敏: ",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = cardContent
                            )
                            Text(
                                member.allergy,
                                style = MaterialTheme.typography.bodySmall,
                                color = cardContent.copy(alpha = 0.8f)
                            )
                        }
                    }
                    if (member.chronic.isNotBlank()) {
                        Row {
                            Text(
                                "慢病: ",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = cardContent
                            )
                            Text(
                                member.chronic,
                                style = MaterialTheme.typography.bodySmall,
                                color = cardContent.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // 最近就诊
                    if (recentRecords.isNotEmpty()) {
                        Text(
                            "最近就诊",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cardContent
                        )
                        recentRecords.take(2).forEach { record ->
                            Text(
                                "${record.onsetTime.format(DateTimeFormatter.ofPattern("MM-dd"))} ${record.diagnosis}",
                                style = MaterialTheme.typography.bodySmall,
                                color = cardContent.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                    }

                    // 操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("编辑", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = onAddRecord,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("添加记录", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = onDelete,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 32.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
                stringResource(R.string.screen_family_health_overview),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    icon = "👥",
                    value = "${members.size}",
                    label = stringResource(R.string.screen_family_stat_members),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = "🏥",
                    value = "$chronicCount",
                    label = stringResource(R.string.screen_family_stat_chronic),
                    color = Reminder,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = "⚠️",
                    value = "$allergyCount",
                    label = stringResource(R.string.screen_family_stat_allergy),
                    color = if (allergyCount > 0) MaterialTheme.colorScheme.error else NoStatus,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = "📋",
                    value = "$recentRecordCount",
                    label = stringResource(R.string.screen_family_stat_monthly_records),
                    color = Healthy,
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
