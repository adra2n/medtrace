package com.yy.medtrace.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.yy.medtrace.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.animation.AnimatedVisibility
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import com.yy.medtrace.data.backup.BackupRepository
import com.yy.medtrace.viewmodel.SettingsViewModel
import com.yy.medtrace.data.backup.buildRecordsCsv
import com.yy.medtrace.data.backup.decodeBackup
import com.yy.medtrace.data.backup.encodeBackup
import com.yy.medtrace.data.backup.shareCsvIntent
import com.yy.medtrace.data.model.UserSettings
import com.yy.medtrace.data.settings.LlmSettingsStore
import com.yy.medtrace.data.settings.UserMode
import com.yy.medtrace.data.settings.UserModeStore
import com.yy.medtrace.data.settings.AiServiceManager
import com.yy.medtrace.navigation.Screen
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userModeStore = remember { UserModeStore(context) }
    val aiServiceManager = remember { AiServiceManager(context) }
    val llmSettings = remember { LlmSettingsStore(context) }
    var settings by remember { mutableStateOf<UserSettings?>(null) }
    
    val currentMode by userModeStore.currentMode.collectAsState()
    val currentAiService by aiServiceManager.currentServiceId.collectAsState()
    val allAiServices = remember { aiServiceManager.getAllServices() }
    
    var darkMode by remember { mutableStateOf(false) }
    var appearanceExpanded by remember { mutableStateOf(true) }
    var aiExpanded by remember { mutableStateOf(false) }
    var backupExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }
    
    var llmApiKey by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }

    val backupRepository = remember { BackupRepository(viewModel.database) }

    LaunchedEffect(Unit) {
        viewModel.database.userSettingsDao().getUserSettings().collect { userSettings ->
            settings = userSettings ?: UserSettings()
            darkMode = userSettings?.darkMode ?: false
        }
    }

    LaunchedEffect(Unit) {
        llmApiKey = aiServiceManager.getApiKey(currentAiService) ?: ""
    }

    fun backToPrevious() {
        if (!navController.popBackStack()) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val json = encodeBackup(backupRepository.exportAll())
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray(Charsets.UTF_8))
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.readText()
                    ?: throw IllegalStateException("无法读取文件")
                val data = decodeBackup(content)
                backupRepository.importAll(data)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "设置",
                navigationIcon = {
                    IconButton(onClick = { backToPrevious() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
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
            // 🎨 外观设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.large,
                colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { appearanceExpanded = !appearanceExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "外观",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            if (appearanceExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = appearanceExpanded) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            SettingsRow(
                                label = "深色模式",
                                trailing = {
                                    Switch(
                                        checked = darkMode,
                                        onCheckedChange = { isChecked ->
                                            darkMode = isChecked
                                            scope.launch {
                                                viewModel.database.userSettingsDao()
                                                    .insertOrUpdate((settings ?: UserSettings()).copy(darkMode = isChecked))
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 🤖 AI配置
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.large,
                colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { aiExpanded = !aiExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "AI配置",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            if (aiExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = aiExpanded) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "选择AI服务",
                                style = MaterialTheme.typography.labelMedium
                            )
                            allAiServices.forEach { service ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            aiServiceManager.selectService(service.id)
                                            scope.launch {
                                                llmApiKey = aiServiceManager.getApiKey(service.id) ?: ""
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = currentAiService == service.id,
                                        onClick = {
                                            aiServiceManager.selectService(service.id)
                                            scope.launch {
                                                llmApiKey = aiServiceManager.getApiKey(service.id) ?: ""
                                            }
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(service.name)
                                    if (service.isBuiltIn) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = AppShapes.small,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                "内置",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                            
                            HorizontalDivider()
                            
                            OutlinedTextField(
                                value = llmApiKey,
                                onValueChange = { llmApiKey = it },
                                label = { Text("API Key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = if (showApiKey) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            imageVector = if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (showApiKey) "隐藏" else "显示"
                                        )
                                    }
                                }
                            )
                            
                            Button(
                                onClick = {
                                    aiServiceManager.setApiKey(currentAiService, llmApiKey)
                                    Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("保存API Key")
                            }
                        }
                    }
                }
            }

            // 💾 数据备份
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.large,
                colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { backupExpanded = !backupExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "数据备份",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            if (backupExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = backupExpanded) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "导出备份将生成一个JSON文件，包含所有数据",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val time = java.time.LocalDateTime.now()
                                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                                        exportLauncher.launch("medtrace_backup_$time.json")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("导出备份")
                                }
                                OutlinedButton(
                                    onClick = { showImportConfirm = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("导入备份")
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val csv = buildRecordsCsv(viewModel.database)
                                            withContext(Dispatchers.Main) {
                                                context.startActivity(
                                                    android.content.Intent.createChooser(
                                                        shareCsvIntent(context, csv),
                                                        "导出CSV"
                                                    )
                                                )
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("导出CSV")
                            }
                        }
                    }
                }
            }

            // ⚙️ 界面模式
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.large,
                colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { modeExpanded = !modeExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "界面模式",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            if (modeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = modeExpanded) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { userModeStore.setMode(UserMode.STANDARD) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentMode == UserMode.STANDARD,
                                    onClick = { userModeStore.setMode(UserMode.STANDARD) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("标准版")
                                    Text(
                                        "完整功能，适合普通用户",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { userModeStore.setMode(UserMode.ELDERLY) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentMode == UserMode.ELDERLY,
                                    onClick = { userModeStore.setMode(UserMode.ELDERLY) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("长辈版")
                                    Text(
                                        "简化操作，大字体、大按钮",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                "切换后立即生效，数据完全共享",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("导入确认") },
            text = { Text("导入将覆盖现有数据，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/json"))
                }) { Text("继续") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        trailing()
    }
}
