package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.UserSettings
import com.yy.chiyaole.data.settings.LlmSettingsStore
import com.yy.chiyaole.ui.theme.cardContainerColor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    database: AppDatabase
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val llmSettings = remember { LlmSettingsStore(context) }
    var settings by remember { mutableStateOf<UserSettings?>(null) }
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var showMemberDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<FamilyMember?>(null) }
    var memberName by remember { mutableStateOf("") }
    var memberRelation by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        database.userSettingsDao().getUserSettings().collect { userSettings ->
            settings = userSettings ?: UserSettings()
        }
    }

    LaunchedEffect(Unit) {
        database.familyMemberDao().getAllMembers()
            .collect { list ->
                members = list
                if (list.isEmpty()) {
                    scope.launch {
                        database.familyMemberDao().insert(
                            FamilyMember(name = "我自己", relation = "本人", isDefault = true)
                        )
                    }
                }
            }
    }

    if (showMemberDialog) {
        AlertDialog(
            onDismissRequest = { showMemberDialog = false },
            title = { Text(if (editingMember == null) "新增家庭成员" else "编辑家庭成员") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = { memberName = it },
                        label = { Text("姓名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = memberRelation,
                        onValueChange = { memberRelation = it },
                        label = { Text("关系（如 本人 / 父亲 / 子女，可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = memberName.isNotBlank(),
                    onClick = {
                        val name = memberName.trim()
                        val relation = memberRelation.trim()
                        val editing = editingMember
                        scope.launch {
                            if (editing == null) {
                                database.familyMemberDao().insert(
                                    FamilyMember(name = name, relation = relation)
                                )
                            } else {
                                database.familyMemberDao().update(
                                    editing.copy(name = name, relation = relation)
                                )
                            }
                        }
                        memberName = ""
                        memberRelation = ""
                        editingMember = null
                        showMemberDialog = false
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showMemberDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI 识别设置
            SettingsSection(title = "AI 识别设置") {
                var llmBaseUrl by remember { mutableStateOf("") }
                var llmApiKey by remember { mutableStateOf("") }
                var llmModel by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    llmBaseUrl = llmSettings.getBaseUrl() ?: ""
                    llmApiKey = llmSettings.getApiKey() ?: ""
                    llmModel = llmSettings.getModel() ?: ""
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "配置你自己的 OpenAI 兼容大模型（Base URL / Key / 模型名）。密钥仅保存在本机。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = llmBaseUrl,
                        onValueChange = {
                            llmBaseUrl = it
                            scope.launch { llmSettings.setBaseUrl(it) }
                        },
                        label = { Text("API Base URL（如 https://api.openai.com/v1）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = llmApiKey,
                        onValueChange = {
                            llmApiKey = it
                            scope.launch { llmSettings.setApiKey(it) }
                        },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = llmModel,
                        onValueChange = {
                            llmModel = it
                            scope.launch { llmSettings.setModel(it) }
                        },
                        label = { Text("模型名（如 gpt-4o）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // 外观设置
            // 家庭成员管理
            SettingsSection(title = "家庭成员管理") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    members.forEach { member ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                val sub = buildList {
                                    if (member.relation.isNotBlank()) add(member.relation)
                                    if (member.isDefault) add("默认")
                                }.joinToString(" · ")
                                Text(
                                    text = if (sub.isBlank()) member.name else "${member.name}（$sub）",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingMember = member
                                        memberName = member.name
                                        memberRelation = member.relation
                                        showMemberDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, "编辑")
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            database.familyMemberDao().deleteById(member.id)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, "删除")
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            editingMember = null
                            memberName = ""
                            memberRelation = ""
                            showMemberDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, "新增")
                        Spacer(Modifier.width(8.dp))
                        Text("新增家庭成员")
                    }
                }
            }

            SettingsSection(title = "外观设置") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("深色模式")
                        Switch(
                            checked = settings?.darkMode ?: false,
                            onCheckedChange = { isChecked ->
                                scope.launch {
                                    settings?.let { currentSettings ->
                                        database.userSettingsDao().insertOrUpdate(
                                            currentSettings.copy(darkMode = isChecked)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = cardContainerColor(),
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}
