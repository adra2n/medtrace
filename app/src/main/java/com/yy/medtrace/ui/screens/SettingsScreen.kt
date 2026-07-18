package com.yy.medtrace.ui.screens

import androidx.compose.foundation.layout.*
import android.content.Intent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.Dispatchers
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.yy.medtrace.BuildConfig
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.backup.BackupRepository
import com.yy.medtrace.data.backup.CryptoUtil
import com.yy.medtrace.data.backup.GistSync
import com.yy.medtrace.data.backup.buildRecordsCsv
import com.yy.medtrace.data.backup.decodeBackup
import com.yy.medtrace.data.backup.encodeBackup
import com.yy.medtrace.data.backup.shareCsvIntent
import com.yy.medtrace.data.model.UserSettings
import com.yy.medtrace.data.settings.LlmSettingsStore
import com.yy.medtrace.data.settings.SecuritySettingsStore
import com.yy.medtrace.Screen
import com.yy.medtrace.data.settings.SyncSettingsStore
import com.yy.medtrace.data.security.BiometricHelper
import com.yy.medtrace.data.security.PinManager
import androidx.fragment.app.FragmentActivity
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.Primary
import com.yy.medtrace.ui.theme.cardContainerColor
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    database: AppDatabase,
    navController: NavController
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

    val backupRepository = remember { BackupRepository(database) }
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
    var appLockEnabled by remember { mutableStateOf(false) }
    var autoLockSeconds by remember { mutableStateOf(0) }
    var secureScreen by remember { mutableStateOf(false) }
    var biometricAvailable by remember { mutableStateOf(false) }
    var pinSet by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(false) }

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
            darkMode = database.userSettingsDao().getUserSettings().first()?.darkMode ?: false
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
            database.userSettingsDao().insertOrUpdate((settings ?: UserSettings()).copy(darkMode = darkMode))
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
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

    // 将文件/网络内容解析为 BackupData：自动识别 ENC: 密文并按加密密码解密
    suspend fun parseBackupContent(content: String): com.yy.medtrace.data.backup.BackupData {
        val text = if (content.startsWith("ENC:")) {
            if (encryptPassword.isBlank()) throw IllegalStateException("该备份已加密，请先在上方填写加密密码")
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
                    Toast.makeText(context, "备份已导出", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_LONG).show()
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
                val data = parseBackupContent(content)
                backupRepository.importAll(data)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "备份已恢复", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "恢复失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun syncToGist() {
        if (githubToken.isBlank()) {
            backupError = "请先填写 GitHub Token"
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
                    Toast.makeText(context, "已同步到 Gist", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "同步失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                busy = false
            }
        }
    }

    fun restoreFromGist() {
        if (githubToken.isBlank()) {
            backupError = "请先填写 GitHub Token"
            return
        }
        if (existingGistId == null) {
            backupError = "尚未同步过 Gist，无可用备份"
            return
        }
        scope.launch(Dispatchers.IO) {
            busy = true
            try {
                val content = GistSync(githubToken).download(existingGistId!!)
                val data = parseBackupContent(content)
                backupRepository.importAll(data)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "已从 Gist 恢复", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "恢复失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                busy = false
            }
        }
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("恢复备份") },
            text = { Text("将用备份文件覆盖当前所有家庭成员与医疗记录，确定继续？") },
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

    backupError?.let { msg ->
        AlertDialog(
            onDismissRequest = { backupError = null },
            title = { Text("提示") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { backupError = null }) { Text("知道了") }
            }
        )
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "设置",
                navigationIcon = {
                    IconButton(onClick = { backToPrevious() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    Button(
                        onClick = { saveAll() },
                        shape = AppShapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Primary
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                    ) {
                        Text("保存", style = MaterialTheme.typography.labelLarge)
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
            // AI 识别设置
            SettingsSection(title = "AI 识别设置") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "配置你自己的 OpenAI 兼容大模型（Base URL / Key / 模型名）。密钥仅保存在本机。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = llmBaseUrl,
                        onValueChange = { llmBaseUrl = it },
                        label = { Text("API Base URL（如 https://api.openai.com/v1）") },
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
                                    contentDescription = if (showApiKey) "隐藏 Key" else "显示 Key"
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = llmModel,
                        onValueChange = { llmModel = it },
                        label = { Text("模型名（如 gpt-4o）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            SettingsSection(title = "外观设置") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsRow(
                        label = "深色模式",
                        trailing = {
                            Switch(
                                checked = darkMode,
                                onCheckedChange = { isChecked ->
                                    darkMode = isChecked
                                    // 即时预览：切换即落库，MainActivity 的主题 Flow 会重新收集并应用。
                                    scope.launch {
                                        database.userSettingsDao()
                                            .insertOrUpdate((settings ?: UserSettings()).copy(darkMode = isChecked))
                                    }
                                }
                            )
                        }
                    )
                }
            }

            SettingsSection(title = "安全") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "开启应用锁后，每次进入或回到医迹都需要验证身份，保护你的家庭医疗数据。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SettingsRow(
                        label = "应用锁（指纹 / 面容 / PIN）",
                        trailing = {
                            Switch(
                                checked = appLockEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        activity?.let {
                                            BiometricHelper.authenticate(
                                                activity = it,
                                                onSuccess = { appLockEnabled = true },
                                                onError = { msg -> backupError = "验证失败：$msg" }
                                            )
                                        }
                                    } else {
                                        appLockEnabled = false
                                    }
                                }
                            )
                        }
                    )

                    if (appLockEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("自动锁定", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val options = listOf(0 to "立即", 60 to "1 分钟后", 300 to "5 分钟后")
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
                            label = "PIN 备用密码",
                            trailing = {
                                TextButton(onClick = { showPinDialog = true }) {
                                    Text(if (pinSet) "清除" else "设置")
                                }
                            }
                        )
                    }

                    SettingsRow(
                        label = "阻止截屏与录屏",
                        trailing = {
                            Switch(
                                checked = secureScreen,
                                onCheckedChange = { checked ->
                                    secureScreen = checked
                                }
                            )
                        }
                    )
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

            SettingsSection(title = "数据备份与恢复") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "将家庭成员与医疗记录导出为文件，或导入此前导出的备份恢复数据。可设置加密密码对备份加密，并同步到 GitHub Gist。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = githubToken,
                        onValueChange = { githubToken = it },
                        label = { Text("GitHub Token（需 gist 权限）") },
                        singleLine = true,
                        visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showToken = !showToken }) {
                                Icon(
                                    imageVector = if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showToken) "隐藏 Token" else "显示 Token"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = encryptPassword,
                        onValueChange = { encryptPassword = it },
                        label = { Text("加密密码（留空则不加密）") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPassword) "隐藏密码" else "显示密码"
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
                        ) { Text("导出备份") }
                        OutlinedButton(
                            onClick = { showImportConfirm = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("导入恢复") }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { syncToGist() },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (existingGistId != null) "更新到 Gist" else "同步到 Gist") }
                        OutlinedButton(
                            onClick = { restoreFromGist() },
                            enabled = !busy && existingGistId != null,
                            modifier = Modifier.weight(1f)
                        ) { Text("从 Gist 恢复") }
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val csv = buildRecordsCsv(database)
                                    withContext(Dispatchers.Main) {
                                        context.startActivity(
                                            Intent.createChooser(
                                                shareCsvIntent(context, csv),
                                                "导出医疗记录 CSV"
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "CSV 导出失败：${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("导出 CSV 报告") }
                }
            }

            SettingsSection(title = "关于") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "医迹",
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
            shape = AppShapes.large,
            color = cardContainerColor(),
            tonalElevation = 0.dp
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
    if (pinSet) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("清除 PIN") },
            text = { Text("确定清除备用 PIN？清除后仅能使用指纹 / 面容解锁。") },
            confirmButton = { TextButton(onClick = onClear) { Text("清除") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
        )
        return
    }

    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置备用 PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (step == 1) "请输入 6 位数字 PIN" else "请再次输入以确认",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = if (step == 1) pin else confirm,
                    onValueChange = { v ->
                        val digits = v.filter { it.isDigit() }.take(8)
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
                enabled = (if (step == 1) pin else confirm).length >= 4,
                onClick = {
                    if (step == 1) {
                        if (pin.length < 4) {
                            error = "PIN 需为 6 位数字"
                            return@TextButton
                        }
                        step = 2
                    } else {
                        if (confirm != pin) {
                            error = "两次输入不一致"
                            confirm = ""
                            return@TextButton
                        }
                        onConfirm(pin)
                    }
                }
            ) { Text(if (step == 1) "下一步" else "确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
