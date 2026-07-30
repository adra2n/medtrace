package com.yy.medtrace.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import android.content.Intent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.yy.medtrace.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.Dispatchers
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.yy.medtrace.BuildConfig
import androidx.hilt.navigation.compose.hiltViewModel
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.backup.BackupRepository
import com.yy.medtrace.viewmodel.SettingsViewModel
import com.yy.medtrace.data.backup.CryptoUtil
import com.yy.medtrace.data.backup.GistSync
import com.yy.medtrace.data.backup.buildRecordsCsv
import com.yy.medtrace.data.backup.decodeBackup
import com.yy.medtrace.data.backup.encodeBackup
import com.yy.medtrace.data.backup.shareCsvIntent
import com.yy.medtrace.data.model.UserSettings
import com.yy.medtrace.data.settings.LlmSettingsStore
import com.yy.medtrace.data.settings.SecuritySettingsStore
import com.yy.medtrace.navigation.Screen
import com.yy.medtrace.data.settings.SyncSettingsStore
import com.yy.medtrace.data.security.BiometricHelper
import com.yy.medtrace.data.security.PinManager
import androidx.fragment.app.FragmentActivity
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val llmSettings = remember { LlmSettingsStore(context) }
    var settings by remember { mutableStateOf<UserSettings?>(null) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var premiumFeatureName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.database.userSettingsDao().getUserSettings().collect { userSettings ->
            settings = userSettings ?: UserSettings()
        }
    }

    val backupRepository = remember { BackupRepository(viewModel.database) }
    val syncSettings = remember { SyncSettingsStore(context) }
    val securitySettings = remember { SecuritySettingsStore(context) }
    var githubToken by remember { mutableStateOf("") }
    var encryptPassword by remember { mutableStateOf("") }
    var existingGistId by remember { mutableStateOf<String?>(null) }
    var llmBaseUrl by remember { mutableStateOf("") }
    var llmApiKey by remember { mutableStateOf("") }
    var llmModel by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var backupError by remember { mutableStateOf<String?>(null) }
    var showToken by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var testingLlm by remember { mutableStateOf(false) }
    var llmTestResult by remember { mutableStateOf<String?>(null) }
    var appLockEnabled by remember { mutableStateOf(false) }
    var autoLockSeconds by remember { mutableStateOf(0) }
    var secureScreen by remember { mutableStateOf(false) }
    var biometricAvailable by remember { mutableStateOf(false) }
    var pinSet by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(false) }

    var appearanceExpanded by remember { mutableStateOf(true) }
    var securityExpanded by remember { mutableStateOf(false) }
    var backupExpanded by remember { mutableStateOf(false) }
    var aiExpanded by remember { mutableStateOf(false) }

    val activity = LocalContext.current as? FragmentActivity

    // 所有可编辑项先写入本地草稿状态，点「保存」才持久化；「取消」则重新从存储加载（放弃修改）
    fun applySecureScreenFlag(enabled: Boolean) {
        activity?.window?.setFlags(
            if (enabled) android.view.WindowManager.LayoutParams.FLAG_SECURE else 0,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    fun loadAll() {
        scope.launch {
            llmSettings.getBaseUrl()?.let { llmBaseUrl = it }
            llmSettings.getApiKey()?.let { llmApiKey = it }
            llmSettings.getModel()?.let { llmModel = it }
            githubToken = syncSettings.getGithubToken() ?: ""
            encryptPassword = syncSettings.getEncryptPassword() ?: ""
            existingGistId = syncSettings.getGistId()
            appLockEnabled = securitySettings.getAppLockEnabled()
            autoLockSeconds = securitySettings.getAutoLockSeconds()
            secureScreen = securitySettings.getSecureScreen()
            applySecureScreenFlag(secureScreen)
            biometricAvailable = activity?.let { BiometricHelper.canAuthenticate(it) } ?: false
            pinSet = activity?.let { PinManager.isPinSet(it) } ?: false
            darkMode = viewModel.database.userSettingsDao().getUserSettings().first()?.darkMode ?: false
        }
    }

    fun backToPrevious() {
        if (!navController.popBackStack()) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        }
    }

    fun saveAll() {
        scope.launch {
            llmSettings.setBaseUrl(llmBaseUrl)
            llmSettings.setApiKey(llmApiKey)
            llmSettings.setModel(llmModel)
            syncSettings.setGithubToken(githubToken)
            syncSettings.setEncryptPassword(encryptPassword)
            existingGistId?.let { syncSettings.setGistId(it) }
            securitySettings.setAppLockEnabled(appLockEnabled)
            securitySettings.setAutoLockSeconds(autoLockSeconds)
            securitySettings.setSecureScreen(secureScreen)
            applySecureScreenFlag(secureScreen)
            viewModel.database.userSettingsDao().insertOrUpdate((settings ?: UserSettings()).copy(darkMode = darkMode))
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.settings_toast_saved), Toast.LENGTH_SHORT).show()
                backToPrevious()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAll()
    }

    // 将备份数据编码为“文件内容”：若设置了加密密码则输出密文（ENC: 前缀）
    suspend fun buildBackupContent(): String {
        val json = encodeBackup(backupRepository.exportAll())
        return if (encryptPassword.isNotBlank()) "ENC:" + CryptoUtil.encrypt(json, encryptPassword)
        else json
    }

    fun testLlmConnection() {
        if (llmBaseUrl.isBlank() || llmApiKey.isBlank() || llmModel.isBlank()) {
            llmTestResult = context.getString(R.string.settings_llm_test_error_incomplete)
            return
        }
        testingLlm = true
        llmTestResult = null
        scope.launch(Dispatchers.IO) {
            try {
                val api = com.yy.medtrace.data.llm.LlmApi.create(llmBaseUrl.trim().removeSuffix("/").let {
                    if (!it.endsWith("/")) "$it/" else it
                })
                val auth = "Bearer ${llmApiKey.trim()}"
                val req = com.yy.medtrace.data.llm.ChatRequest(
                    model = llmModel.trim(),
                    messages = listOf(com.yy.medtrace.data.llm.Message("user", kotlinx.serialization.json.JsonPrimitive("Hi")))
                )
                api.chat(auth, req)
                withContext(Dispatchers.Main) {
                    llmTestResult = context.getString(R.string.settings_llm_test_success)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    llmTestResult = context.getString(R.string.settings_llm_test_error, e.message ?: e.javaClass.simpleName)
                }
            } finally {
                testingLlm = false
            }
        }
    }

    // 将文件/网络内容解析为 BackupData：自动识别 ENC: 密文并按加密密码解密
    suspend fun parseBackupContent(content: String): com.yy.medtrace.data.backup.BackupData {
        val text = if (content.startsWith("ENC:")) {
            if (encryptPassword.isBlank()) throw IllegalStateException(context.getString(R.string.settings_backup_encrypted_error))
            CryptoUtil.decrypt(content.removePrefix("ENC:"), encryptPassword)
        } else {
            content
        }
        return decodeBackup(text)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val content = buildBackupContent()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
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
                val data = parseBackupContent(content)
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

    fun syncToGist() {
        if (githubToken.isBlank()) {
            backupError = context.getString(R.string.settings_error_fill_github_token)
            return
        }
        scope.launch(Dispatchers.IO) {
            busy = true
            try {
                val content = buildBackupContent()
                val id = GistSync(githubToken).upload(content, existingGistId)
                syncSettings.setGistId(id)
                existingGistId = id
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.settings_toast_synced_to_gist), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.settings_toast_sync_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            } finally {
                busy = false
            }
        }
    }

    fun restoreFromGist() {
        if (githubToken.isBlank()) {
            backupError = context.getString(R.string.settings_error_fill_github_token)
            return
        }
        if (existingGistId == null) {
            backupError = context.getString(R.string.settings_error_no_gist_backup)
            return
        }
        scope.launch(Dispatchers.IO) {
            busy = true
            try {
                val content = GistSync(githubToken).download(existingGistId!!)
                val data = parseBackupContent(content)
                backupRepository.importAll(data)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.settings_toast_restored), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.settings_toast_restore_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            } finally {
                busy = false
            }
        }
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text(stringResource(R.string.settings_dialog_restore_title)) },
            text = { Text(stringResource(R.string.settings_dialog_restore_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/json"))
                }) { Text(stringResource(R.string.settings_btn_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    backupError?.let { msg ->
        AlertDialog(
            onDismissRequest = { backupError = null },
            title = { Text(stringResource(R.string.settings_dialog_hint_title)) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { backupError = null }) { Text(stringResource(R.string.settings_btn_got_it)) }
            }
        )
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = stringResource(R.string.settings_title),
                navigationIcon = {
                    IconButton(onClick = { backToPrevious() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_cd_back))
                    }
                },
                actions = {
                    Button(
                        onClick = { saveAll() },
                        shape = AppShapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.btn_save), style = MaterialTheme.typography.labelLarge)
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
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.settings_section_appearance),
                            style = MaterialTheme.typography.titleMedium,
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
                                label = stringResource(R.string.settings_label_dark_mode),
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

            // 🔒 安全与锁屏
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
                            .clickable { securityExpanded = !securityExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.settings_section_security),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            if (securityExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = securityExpanded) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                stringResource(R.string.settings_security_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SettingsRow(
                                label = stringResource(R.string.settings_label_app_lock),
                                trailing = {
                                    Switch(
                                        checked = appLockEnabled,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                activity?.let {
                                                    BiometricHelper.authenticate(
                                                        activity = it,
                                                        onSuccess = { appLockEnabled = true },
                                                        onError = { msg -> backupError = context.getString(R.string.settings_error_auth_failed, msg) }
                                                    )
                                                }
                                            } else {
                                                appLockEnabled = false
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            )

                            if (appLockEnabled) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(stringResource(R.string.settings_label_auto_lock), style = MaterialTheme.typography.labelMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val options = listOf(0 to stringResource(R.string.settings_option_immediately), 60 to stringResource(R.string.settings_option_1_minute), 300 to stringResource(R.string.settings_option_5_minutes))
                                        options.forEach { (sec, label) ->
                                            FilterChip(
                                                selected = autoLockSeconds == sec,
                                                onClick = { autoLockSeconds = sec },
                                                label = { Text(label) }
                                            )
                                        }
                                    }
                                }

                                SettingsRow(
                                    label = stringResource(R.string.settings_label_pin_backup),
                                    trailing = {
                                        TextButton(onClick = { showPinDialog = true }) {
                                            Text(if (pinSet) stringResource(R.string.settings_btn_clear) else stringResource(R.string.settings_btn_set))
                                        }
                                    }
                                )
                            }

                            SettingsRow(
                                label = stringResource(R.string.settings_label_block_screenshot),
                                trailing = {
                                    Switch(
                                        checked = secureScreen,
                                        onCheckedChange = { checked ->
                                            secureScreen = checked
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            if (showPinDialog) {
                PinSetupDialog(
                    pinSet = pinSet,
                    onConfirm = { pin ->
                        activity?.let {
                            PinManager.setPin(it, pin)
                            pinSet = true
                        }
                        showPinDialog = false
                    },
                    onClear = {
                        activity?.let { PinManager.clearPin(it) }
                        pinSet = false
                        showPinDialog = false
                    },
                    onDismiss = { showPinDialog = false }
                )
            }

            // ☁️ 数据备份与同步（高级功能）
            val isPremiumActive = viewModel.premiumManager?.isPremiumActive() ?: false
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremiumActive) cardContainerColor()
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
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
                            Icons.Default.Cloud,
                            contentDescription = null,
                            tint = if (isPremiumActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_title_backup_sync),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isPremiumActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        if (!isPremiumActive) {
                            Surface(
                                shape = AppShapes.small,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    stringResource(R.string.settings_label_premium),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            if (backupExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = backupExpanded) {
                        if (isPremiumActive) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    stringResource(R.string.settings_backup_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = githubToken,
                                    onValueChange = { githubToken = it },
                                    label = { Text(stringResource(R.string.settings_label_github_token)) },
                                    singleLine = true,
                                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showToken = !showToken }) {
                                            Icon(
                                                imageVector = if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (showToken) stringResource(R.string.settings_cd_hide_token) else stringResource(R.string.settings_cd_show_token)
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = encryptPassword,
                                    onValueChange = { encryptPassword = it },
                                    label = { Text(stringResource(R.string.settings_label_encrypt_password)) },
                                    singleLine = true,
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (showPassword) stringResource(R.string.settings_cd_hide_password) else stringResource(R.string.settings_cd_show_password)
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val time = java.time.LocalDateTime.now()
                                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                                            exportLauncher.launch("chiyaole_backup_$time.json")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(stringResource(R.string.settings_btn_export_backup)) }
                                    OutlinedButton(
                                        onClick = { showImportConfirm = true },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(stringResource(R.string.settings_btn_import_restore)) }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { syncToGist() },
                                        enabled = !busy,
                                        modifier = Modifier.weight(1f)
                                    ) { Text(if (existingGistId != null) stringResource(R.string.settings_btn_update_to_gist) else stringResource(R.string.settings_btn_sync_to_gist)) }
                                    OutlinedButton(
                                        onClick = { restoreFromGist() },
                                        enabled = !busy && existingGistId != null,
                                        modifier = Modifier.weight(1f)
                                    ) { Text(stringResource(R.string.settings_btn_restore_from_gist)) }
                                }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val csv = buildRecordsCsv(viewModel.database)
                                                withContext(Dispatchers.Main) {
                                                    context.startActivity(
                                                        Intent.createChooser(
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
                                ) { Text(stringResource(R.string.settings_btn_export_csv)) }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Text(
                                    stringResource(R.string.settings_premium_unlock_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(onClick = { navController.navigate("premium") }) {
                                    Text(stringResource(R.string.settings_btn_upgrade_price))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // 🤖 AI 配置（高级功能）
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremiumActive) cardContainerColor()
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
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
                            tint = if (isPremiumActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_title_ai_config),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isPremiumActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        if (!isPremiumActive) {
                            Surface(
                                shape = AppShapes.small,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    stringResource(R.string.settings_label_premium),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            if (aiExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = aiExpanded) {
                        if (isPremiumActive) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    stringResource(R.string.settings_ai_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    stringResource(R.string.settings_ai_privacy_warning),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                                OutlinedTextField(
                                    value = llmBaseUrl,
                                    onValueChange = { llmBaseUrl = it },
                                    label = { Text("API Base URL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = llmApiKey,
                                    onValueChange = { llmApiKey = it },
                                    label = { Text("API Key") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showApiKey = !showApiKey }) {
                                            Icon(
                                                imageVector = if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (showApiKey) stringResource(R.string.settings_cd_hide_key) else stringResource(R.string.settings_cd_show_key)
                                            )
                                        }
                                    }
                                )
                                OutlinedTextField(
                                    value = llmModel,
                                    onValueChange = { llmModel = it },
                                    label = { Text(stringResource(R.string.settings_label_model_name)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedButton(
                                    onClick = { testLlmConnection() },
                                    enabled = !testingLlm && llmBaseUrl.isNotBlank() && llmApiKey.isNotBlank() && llmModel.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (testingLlm) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(stringResource(R.string.settings_llm_test_btn))
                                }
                                llmTestResult?.let { result ->
                                    val isError = result.startsWith("❌")
                                    Text(
                                        result,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Text(
                                    stringResource(R.string.settings_premium_unlock_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(onClick = { navController.navigate("premium") }) {
                                    Text(stringResource(R.string.settings_btn_upgrade_price))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

        }
    }

    // 高级版提示对话框
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = { Text(stringResource(R.string.settings_dialog_premium_title)) },
            text = { Text(stringResource(R.string.settings_dialog_premium_message, premiumFeatureName)) },
            confirmButton = {
                TextButton(onClick = {
                    showPremiumDialog = false
                    navController.navigate("premium")
                }) {
                    Text(stringResource(R.string.settings_btn_upgrade))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumDialog = false }) {
                    Text(stringResource(R.string.settings_btn_later))
                }
            }
        )
    }
}

@Composable
fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
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
            shape = AppShapes.large,
            color = cardContainerColor(),
            shadowElevation = SoftElevation
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        trailing()
    }
}

@Composable
fun PinSetupDialog(
    pinSet: Boolean,
    onConfirm: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    if (pinSet) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_dialog_clear_pin_title)) },
            text = { Text(stringResource(R.string.settings_dialog_clear_pin_message)) },
            confirmButton = { TextButton(onClick = onClear) { Text(stringResource(R.string.settings_btn_clear)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
        )
        return
    }

    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_dialog_set_pin_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (step == 1) stringResource(R.string.settings_pin_step1_hint) else stringResource(R.string.settings_pin_step2_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = if (step == 1) pin else confirm,
                    onValueChange = { v ->
                        val digits = v.filter { it.isDigit() }.take(6)
                        error = null
                        if (step == 1) pin = digits else confirm = digits
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = (if (step == 1) pin else confirm).length == 6,
                onClick = {
                    if (step == 1) {
                        if (pin.length != 6) {
                            error = context.getString(R.string.settings_error_pin_not_6_digits)
                            return@TextButton
                        }
                        step = 2
                    } else {
                        if (confirm != pin) {
                            error = context.getString(R.string.settings_error_pin_mismatch)
                            confirm = ""
                            return@TextButton
                        }
                        onConfirm(pin)
                    }
                }
            ) { Text(if (step == 1) stringResource(R.string.settings_btn_next_step) else stringResource(R.string.settings_btn_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}
