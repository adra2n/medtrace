package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.UserSettings
import com.yy.chiyaole.util.ReminderPreviewUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    var settings by remember { mutableStateOf<UserSettings?>(null) }
    var showAdvanceTimeDialog by remember { mutableStateOf(false) }
    
    val reminderPreviewUtil = remember { ReminderPreviewUtil(context) }

    // 在组件销毁时释放资源
    DisposableEffect(Unit) {
        onDispose {
            reminderPreviewUtil.release()
        }
    }

    // 加载设置
    LaunchedEffect(Unit) {
        database.userSettingsDao().getUserSettings().collect { userSettings ->
            settings = userSettings ?: UserSettings()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 提醒设置
                item {
                    SettingsSection(title = "提醒设置") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("提前提醒时间")
                                TextButton(onClick = { showAdvanceTimeDialog = true }) {
                                    Text("${settings?.reminderAdvanceMinutes ?: 30}分钟")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("语音提醒")
                                Switch(
                                    checked = settings?.enableVoiceReminder ?: false,
                                    onCheckedChange = { isChecked ->
                                        scope.launch {
                                            settings?.let { currentSettings ->
                                                database.userSettingsDao().insertOrUpdate(
                                                    currentSettings.copy(enableVoiceReminder = isChecked)
                                                )
                                            }
                                        }
                                        if (isChecked) {
                                            reminderPreviewUtil.previewVoiceReminder()
                                        }
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("通知声音")
                                Switch(
                                    checked = settings?.enableNotificationSound ?: false,
                                    onCheckedChange = { isChecked ->
                                        scope.launch {
                                            settings?.let { currentSettings ->
                                                database.userSettingsDao().insertOrUpdate(
                                                    currentSettings.copy(enableNotificationSound = isChecked)
                                                )
                                            }
                                        }
                                        if (isChecked) {
                                            reminderPreviewUtil.previewNotificationSound()
                                        }
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("震动")
                                Switch(
                                    checked = settings?.enableVibration ?: false,
                                    onCheckedChange = { isChecked ->
                                        scope.launch {
                                            settings?.let { currentSettings ->
                                                database.userSettingsDao().insertOrUpdate(
                                                    currentSettings.copy(enableVibration = isChecked)
                                                )
                                            }
                                        }
                                        if (isChecked) {
                                            reminderPreviewUtil.previewVibration()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // 外观设置
                item {
                    SettingsSection(title = "外观设置") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )

    if (showAdvanceTimeDialog) {
        AlertDialog(
            onDismissRequest = { showAdvanceTimeDialog = false },
            title = { Text("提前提醒时间") },
            text = {
                NumberPicker(
                    value = settings?.reminderAdvanceMinutes ?: 30,
                    onValueChange = { minutes ->
                        scope.launch {
                            settings?.let { currentSettings ->
                                database.userSettingsDao().insertOrUpdate(
                                    currentSettings.copy(reminderAdvanceMinutes = minutes)
                                )
                            }
                        }
                    },
                    range = 1..60
                )
            },
            confirmButton = {
                TextButton(onClick = { showAdvanceTimeDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    var tempValue by remember { mutableStateOf(value) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$tempValue 分钟", style = MaterialTheme.typography.headlineMedium)
        Slider(
            value = tempValue.toFloat(),
            onValueChange = { 
                tempValue = it.toInt()
                onValueChange(it.toInt())
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1
        )
    }
}
