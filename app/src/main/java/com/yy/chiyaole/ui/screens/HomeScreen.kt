package com.yy.chiyaole.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.data.model.UserSettings
import com.yy.chiyaole.ui.components.EmptyRecords
import com.yy.chiyaole.ui.components.HealthTipsCard
import com.yy.chiyaole.ui.components.MedicalRecordCard
import com.yy.chiyaole.ui.components.MemberSelector
import com.yy.chiyaole.ui.components.SectionCard
import com.yy.chiyaole.ui.components.InfoRow
import com.yy.chiyaole.ui.theme.AppShapes
import com.yy.chiyaole.ui.theme.cardContainerColor
import com.yy.chiyaole.SettingsAction
import com.yy.chiyaole.ui.state.SelectedMemberHolder
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    database: AppDatabase,
    navController: NavController
) {
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var recentRecords by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var tipRecords by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val selectedMemberId = SelectedMemberHolder.homeSelectedMemberId.value

    LaunchedEffect(Unit) {
        database.familyMemberDao().getAllMembers()
            .catch { e -> error = e.message }
            .collect { list ->
                if (list.isEmpty()) {
                    scope.launch {
                        database.familyMemberDao().insert(
                            FamilyMember(name = "我自己", relation = "本人", isDefault = true)
                        )
                    }
                    return@collect
                }
                members = list
                val persisted = database.userSettingsDao().getUserSettings().firstOrNull()?.selectedMemberId
                val validPersisted = if (persisted != null && persisted != 0L && list.any { it.id == persisted }) persisted else null
                if (SelectedMemberHolder.homeSelectedMemberId.value == null) {
                    SelectedMemberHolder.homeSelectedMemberId.value =
                        validPersisted ?: (database.medicalRecordDao().getLatestRecord()?.patientId ?: list.first().id)
                }
            }
    }

    LaunchedEffect(selectedMemberId) {
        selectedMemberId?.let { id ->
            database.medicalRecordDao().getRecentRecordsByMember(id, 1)
                .catch { e -> error = e.message }
                .collect { records -> recentRecords = records }
            database.medicalRecordDao().getRecentRecordsByMember(id, 12)
                .catch { e -> error = e.message }
                .collect { records -> tipRecords = records }
        } ?: run {
            recentRecords = emptyList()
            tipRecords = emptyList()
        }
    }

    val currentMember = members.firstOrNull { it.id == selectedMemberId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "医迹",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "家庭健康管理",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = { SettingsAction(navController) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                MemberSelector(
                    members = members,
                    selectedMemberId = selectedMemberId,
                    onSelect = { member ->
                        SelectedMemberHolder.homeSelectedMemberId.value = member.id
                        scope.launch {
                            database.userSettingsDao().getUserSettings().firstOrNull()?.let { s ->
                                database.userSettingsDao().insertOrUpdate(s.copy(selectedMemberId = member.id))
                            }
                        }
                    }
                )
            }

            item {
                Card(
                    onClick = { navController.navigate("trends") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("健康趋势", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "查看 AI 识别的血压、血糖等指标变化",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = "进入",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            currentMember?.let { member ->
                item {
                    SectionCard(title = "个人信息") {
                        InfoRow("姓名", member.name)
                        if (member.relation.isNotBlank()) InfoRow("关系", member.relation)
                        if (member.gender.isNotBlank()) InfoRow("性别", member.gender)
                        if (member.birthday.isNotBlank()) InfoRow("生日", member.birthday)
                        if (member.bloodType.isNotBlank()) InfoRow("血型", member.bloodType)
                    }
                }

                item {
                    val notes = buildList {
                        if (member.allergy.isNotBlank()) add("过敏史" to member.allergy)
                        if (member.chronic.isNotBlank()) add("慢性病" to member.chronic)
                        if (member.medicationNote.isNotBlank()) add("用药注意" to member.medicationNote)
                        if (member.otherNote.isNotBlank()) add("其他备注" to member.otherNote)
                    }
                    if (notes.isNotEmpty()) {
                        SectionCard(title = "医疗注意事项") {
                            notes.forEach { (label, value) -> InfoRow(label, value) }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "最新医疗记录",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { navController.navigate("medical_records") }) {
                        Text("查看全部")
                    }
                }
            }

            if (recentRecords.isEmpty()) {
                item { EmptyRecords(onAdd = { navController.navigate("add_record") }) }
            } else {
                items(recentRecords) { record ->
                    MedicalRecordCard(record)
                }
            }

            item {
                HealthTipsCard(member = currentMember, recentRecords = tipRecords)
            }
        }
    }
}
