package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yy.chiyaole.BuildConfig
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.UserSettings
import com.yy.chiyaole.data.settings.LlmSettingsStore
import com.yy.chiyaole.ui.theme.cardContainerColor
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

    LaunchedEffect(Unit) {
        database.userSettingsDao().getUserSettings().collect { userSettings ->
            settings = userSettings ?: UserSettings()
        }
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

            SettingsSection(title = "关于") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "智药乐",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "家庭医疗记录管理工具：拍照/粘贴即可用 AI 提取诊断、用药与医院信息，按家庭成员归类管理。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "版本 ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
