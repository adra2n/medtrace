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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
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
import com.yy.medtrace.data.settings.UserMode
import com.yy.medtrace.data.settings.UserModeStore
import com.yy.medtrace.data.settings.AiService
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
    navController: NavController,
    userModeStore: com.yy.medtrace.data.settings.UserModeStore
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiServiceManager = remember { AiServiceManager(context) }
    var settings by remember { mutableStateOf<UserSettings?>(null) }
    
    val currentMode by userModeStore.currentMode.collectAsState()
    val currentAiService by aiServiceManager.currentServiceId.collectAsState()
    val customServices by aiServiceManager.services.collectAsState()
    val allAiServices = AiServiceManager.BUILT_IN_SERVICES + customServices
    val currentService = remember(currentAiService, customServices) {
        allAiServices.find { it.id == currentAiService }
    }
    
    var darkMode by remember { mutableStateOf(false) }
    var appearanceExpanded by remember { mutableStateOf(true) }
    var aiExpanded by remember { mutableStateOf(false) }
    var backupExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }
    
    var llmApiKey by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }

    // 自定义服务（provider）添加/编辑
    var showProviderDialog by remember { mutableStateOf(false) }
    var editingProvider by remember { mutableStateOf<com.yy.medtrace.data.settings.AiService?>(null) }
    var providerName by remember { mutableStateOf("") }
    var providerBaseUrl by remember { mutableStateOf("") }
    var providerModel by remember { mutableStateOf("") }
    var providerNameError by remember { mutableStateOf<String?>(null) }
    var providerUrlError by remember { mutableStateOf<String?>(null) }
    var showDeleteProviderConfirm by remember { mutableStateOf<com.yy.medtrace.data.settings.AiService?>(null) }

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
                    Toast.makeText(context, context.getString(R.string.settings_toast_exported), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.settings_toast_export_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
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
                    ?: throw IllegalStateException(context.getString(R.string.settings_error_cannot_read_file))
                val data = decodeBackup(content)
                backupRepository.importAll(data)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.settings_toast_restored), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.settings_toast_restore_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
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
            // 外观设置
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

            // AI配置
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
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            aiServiceManager.selectService(service.id)
                                            scope.launch {
                                                llmApiKey = aiServiceManager.getApiKey(service.id) ?: ""
                                            }
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
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
                                        Text(service.name, modifier = Modifier.weight(1f))
                                        if (service.isBuiltIn) {
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
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    editingProvider = service
                                                    providerName = service.name
                                                    providerBaseUrl = service.baseUrl
                                                    providerModel = service.model
                                                    providerNameError = null
                                                    providerUrlError = null
                                                    showProviderDialog = true
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Filled.Edit,
                                                    contentDescription = stringResource(R.string.settings_btn_edit_provider),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { showDeleteProviderConfirm = service }
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = stringResource(R.string.settings_btn_delete_provider),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                    if (!service.isBuiltIn) {
                                        Text(
                                            service.baseUrl.ifBlank { "未设置 Base URL" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 40.dp, top = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    editingProvider = null
                                    providerName = ""
                                    providerBaseUrl = ""
                                    providerModel = ""
                                    providerNameError = null
                                    providerUrlError = null
                                    showProviderDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.settings_btn_add_provider))
                            }

                            Spacer(Modifier.height(8.dp))
                            currentService?.let { svc ->
                                if (!svc.isBuiltIn) {
                                    Text(
                                        stringResource(R.string.settings_provider_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Base URL：${svc.baseUrl.ifBlank { "未设置" }}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "模型：${svc.model.ifBlank { "默认 gpt-4o" }}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        "当前：${svc.name}（内置服务 · 模型 ${svc.model}）",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider()
                            
                            OutlinedTextField(
                                value = llmApiKey,
                                onValueChange = { llmApiKey = it },
                                label = { Text(stringResource(R.string.settings_label_api_key)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = if (showApiKey) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            imageVector = if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (showApiKey) stringResource(R.string.settings_cd_hide_key) else stringResource(R.string.settings_cd_show_key)
                                        )
                                    }
                                }
                            )
                            
                            Button(
                                onClick = {
                                    aiServiceManager.setApiKey(currentAiService, llmApiKey)
                                    Toast.makeText(context, context.getString(R.string.settings_toast_saved), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.settings_btn_save_api_key))
                            }
                        }
                    }
                }
            }

            // 数据备份
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
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
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
                                                        context.getString(R.string.settings_chooser_export_csv)
                                                    )
                                                )
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, context.getString(R.string.settings_toast_csv_export_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.settings_btn_export_csv))
                            }
                        }
                    }
                }
            }

            // 界面模式
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
            title = { Text(stringResource(R.string.settings_dialog_import_title)) },
            text = { Text(stringResource(R.string.settings_dialog_import_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/json"))
                }) { Text(stringResource(R.string.settings_btn_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text(stringResource(R.string.settings_btn_cancel)) }
            }
        )
    }

    // 添加 / 编辑自定义 AI 服务（provider）
    if (showProviderDialog) {
        AlertDialog(
            onDismissRequest = { showProviderDialog = false },
            title = {
                Text(
                    stringResource(
                        if (editingProvider != null) R.string.settings_dialog_edit_provider_title
                        else R.string.settings_dialog_add_provider_title
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = providerName,
                        onValueChange = { providerName = it; providerNameError = null },
                        label = { Text(stringResource(R.string.settings_label_provider_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = providerNameError != null,
                        supportingText = { providerNameError?.let { Text(it) } }
                    )
                    OutlinedTextField(
                        value = providerBaseUrl,
                        onValueChange = { providerBaseUrl = it; providerUrlError = null },
                        label = { Text(stringResource(R.string.settings_label_base_url)) },
                        placeholder = { Text("https://api.example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = providerUrlError != null,
                        supportingText = { providerUrlError?.let { Text(it) } }
                    )
                    OutlinedTextField(
                        value = providerModel,
                        onValueChange = { providerModel = it },
                        label = { Text(stringResource(R.string.settings_label_model_name)) },
                        placeholder = { Text("gpt-4o") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = providerName.trim()
                    val url = providerBaseUrl.trim()
                    var valid = true
                    if (name.isEmpty()) {
                        providerNameError = context.getString(R.string.settings_error_provider_name_required)
                        valid = false
                    }
                    if (url.isEmpty()) {
                        providerUrlError = context.getString(R.string.settings_error_provider_url_required)
                        valid = false
                    }
                    if (!valid) return@TextButton
                    val id = editingProvider?.id ?: ("custom_" + System.currentTimeMillis())
                    val svc = AiService(id, name, url, providerModel.trim(), false)
                    aiServiceManager.addCustomService(svc)
                    aiServiceManager.selectService(id)
                    llmApiKey = aiServiceManager.getApiKey(id) ?: ""
                    showProviderDialog = false
                    Toast.makeText(
                        context,
                        context.getString(
                            if (editingProvider != null) R.string.settings_toast_provider_updated
                            else R.string.settings_toast_provider_added
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }) { Text(stringResource(R.string.settings_btn_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showProviderDialog = false }) { Text(stringResource(R.string.settings_btn_cancel)) }
            }
        )
    }

    // 删除自定义服务确认
    showDeleteProviderConfirm?.let { svc ->
        AlertDialog(
            onDismissRequest = { showDeleteProviderConfirm = null },
            title = { Text(stringResource(R.string.settings_dialog_delete_provider_title)) },
            text = { Text(stringResource(R.string.settings_dialog_delete_provider_message, svc.name)) },
            confirmButton = {
                TextButton(onClick = {
                    aiServiceManager.removeCustomService(svc.id)
                    scope.launch {
                        llmApiKey = aiServiceManager.getApiKey(aiServiceManager.currentServiceId.value) ?: ""
                    }
                    showDeleteProviderConfirm = null
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_toast_provider_removed),
                        Toast.LENGTH_SHORT
                    ).show()
                }) { Text(stringResource(R.string.settings_btn_delete_provider)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteProviderConfirm = null }) { Text(stringResource(R.string.settings_btn_cancel)) }
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
