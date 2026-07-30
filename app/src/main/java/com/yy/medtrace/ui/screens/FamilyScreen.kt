package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
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
                item {
                    HealthDashboard(
                        members = members,
                        recentRecordCount = recentRecordCount
                    )
                }
                item {
                    Text(
                        stringResource(R.string.screen_family_member_list),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
                            navController.navigate("add_record")
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MemberAvatar(
                    member = member,
                    size = if (isExpanded) 48.dp else 36.dp,
                    fallbackBackground = cardContent.copy(alpha = 0.18f),
                    fallbackContent = cardContent
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
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
                                    stringResource(R.string.screen_family_default_tag),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cardContent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (!isExpanded) {
                        val sub = buildList {
                            if (member.relation.isNotBlank()) add(member.relation)
                            age?.let { add(stringResource(R.string.screen_family_age_format, it)) }
                        }.joinToString(" · ")
                        if (sub.isNotBlank()) {
                            Text(
                                sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = cardContent.copy(alpha = 0.8f)
                            )
                        }
                    }
                    if (isExpanded) {
                        val sub = buildList {
                            if (member.relation.isNotBlank()) add(member.relation)
                            age?.let { add(stringResource(R.string.screen_family_age_format, it)) }
                            if (member.gender.isNotBlank()) add(member.gender)
                            if (member.bloodType.isNotBlank()) add(stringResource(R.string.screen_family_blood_type_format, member.bloodType))
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
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess
                                     else Icons.Default.ExpandMore,
                        contentDescription = stringResource(if (isExpanded) R.string.screen_family_collapse else R.string.screen_family_expand),
                        tint = cardContent.copy(alpha = 0.6f)
                    )
                }
            }

            if (!isExpanded) {
                val healthTags = buildList {
                    member.allergy.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        .forEach { add(stringResource(R.string.screen_family_allergy_tag_format, it)) }
                    member.chronic.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        .forEach { add(it) }
                }
                if (healthTags.isNotEmpty()) {
                    val tagColors = healthTagColorSets()
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        healthTags.forEach { tag ->
                            val isAllergy = tag.startsWith("过敏:")
                            val colorSet = if (isAllergy) tagColors["allergy"]!! else tagColors["chronic"]!!
                            Surface(
                                shape = AppShapes.small,
                                color = colorSet.bg
                            ) {
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorSet.content,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (member.allergy.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.screen_family_allergy_label),
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
                                stringResource(R.string.screen_family_chronic_label),
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
                                stringResource(R.string.screen_family_medication_label),
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

                if (recentRecords.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            stringResource(R.string.screen_family_recent_visits),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.screen_family_record_count_format, recordCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = cardContent.copy(alpha = 0.7f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.screen_family_edit_content_desc), modifier = Modifier.size(16.dp), tint = cardContent)
                        }
                        IconButton(
                            onClick = onAddRecord,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.screen_family_add_record_content_desc), modifier = Modifier.size(16.dp), tint = cardContent)
                        }
                        IconButton(
                            onClick = onViewRecords,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = stringResource(R.string.screen_family_view_records_content_desc), modifier = Modifier.size(16.dp), tint = cardContent)
                        }
                    }
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.screen_family_delete_member_button), style = MaterialTheme.typography.labelSmall)
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
