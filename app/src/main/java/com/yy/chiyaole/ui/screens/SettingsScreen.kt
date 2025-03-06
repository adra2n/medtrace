package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.UserSettings
import com.yy.chiyaole.utils.ReminderPreviewUtil
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    var settings by remember { mutableStateOf<UserSettings?>(null) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showAdvanceTimeDialog by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 睡眠时间设置
        SettingsSection(title = "睡眠时间") {
            settings?.let { currentSettings ->
                // 睡眠开始时间
                OutlinedCard(
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null)
                            Text("睡眠开始时间")
                        }
                        Text(currentSettings.sleepStartTime.format(timeFormatter))
                    }
                }

                // 睡眠结束时间
                OutlinedCard(
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null)
                            Text("睡眠结束时间")
                        }
                        Text(currentSettings.sleepEndTime.format(timeFormatter))
                    }
                }
            }
        }

        // 提醒设置
        SettingsSection(title = "提醒设置") {
            settings?.let { currentSettings ->
                // 提前提醒时间
                OutlinedCard(
                    onClick = { showAdvanceTimeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null)
                            Text("提前提醒时间")
                        }
                        Text("${currentSettings.reminderAdvanceMinutes}分钟")
                    }
                }

                // 语音提醒开关
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (currentSettings.enableVoiceReminder) {
                            reminderPreviewUtil.previewVoiceReminder()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Text("语音提醒")
                        }
                        Switch(
                            checked = currentSettings.enableVoiceReminder,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    database.userSettingsDao().insertOrUpdate(
                                        currentSettings.copy(enableVoiceReminder = checked)
                                    )
                                }
                                if (checked) {
                                    reminderPreviewUtil.previewVoiceReminder()
                                }
                            }
                        )
                    }
                }

                // 通知声音开关
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (currentSettings.enableNotificationSound) {
                            reminderPreviewUtil.previewNotificationSound()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null)
                            Text("通知声音")
                        }
                        Switch(
                            checked = currentSettings.enableNotificationSound,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    database.userSettingsDao().insertOrUpdate(
                                        currentSettings.copy(enableNotificationSound = checked)
                                    )
                                }
                                if (checked) {
                                    reminderPreviewUtil.previewNotificationSound()
                                }
                            }
                        )
                    }
                }

                // 震动开关
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (currentSettings.enableVibration) {
                            reminderPreviewUtil.previewVibration()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null)
                            Text("震动")
                        }
                        Switch(
                            checked = currentSettings.enableVibration,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    database.userSettingsDao().insertOrUpdate(
                                        currentSettings.copy(enableVibration = checked)
                                    )
                                }
                                if (checked) {
                                    reminderPreviewUtil.previewVibration()
                                }
                            }
                        )
                    }
                }
            }
        }

        // 外观设置
        SettingsSection(title = "外观设置") {
            settings?.let { currentSettings ->
                // 深色模式开关
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Favorite , contentDescription = null)
                            Text("深色模式")
                        }
                        Switch(
                            checked = currentSettings.darkMode,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    database.userSettingsDao().insertOrUpdate(
                                        currentSettings.copy(darkMode = checked)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 时间选择器对话框
    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                scope.launch {
                    settings?.let { currentSettings ->
                        val newStartTime = LocalTime.of(hour, minute)
                        database.userSettingsDao().insertOrUpdate(
                            currentSettings.copy(sleepStartTime = newStartTime)
                        )
                    }
                }
                showStartTimePicker = false
            },
            initialHour = settings?.sleepStartTime?.hour ?: 22,
            initialMinute = settings?.sleepStartTime?.minute ?: 0
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                scope.launch {
                    settings?.let { currentSettings ->
                        val newEndTime = LocalTime.of(hour, minute)
                        database.userSettingsDao().insertOrUpdate(
                            currentSettings.copy(sleepEndTime = newEndTime)
                        )
                    }
                }
                showEndTimePicker = false
            },
            initialHour = settings?.sleepEndTime?.hour ?: 6,
            initialMinute = settings?.sleepEndTime?.minute ?: 0
        )
    }

    // 提前提醒时间设置对话框
    if (showAdvanceTimeDialog) {
        var minutes by remember(showAdvanceTimeDialog) { 
            mutableStateOf(settings?.reminderAdvanceMinutes?.toString() ?: "5") 
        }
        AlertDialog(
            onDismissRequest = { showAdvanceTimeDialog = false },
            title = { Text("设置提前提醒时间") },
            text = {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { if (it.all { char -> char.isDigit() }) minutes = it },
                    label = { Text("分钟") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            settings?.let { currentSettings ->
                                database.userSettingsDao().insertOrUpdate(
                                    currentSettings.copy(
                                        reminderAdvanceMinutes = minutes.toIntOrNull() ?: 5
                                    )
                                )
                            }
                        }
                        showAdvanceTimeDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdvanceTimeDialog = false }) {
                    Text("取消")
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
        content()
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    initialHour: Int,
    initialMinute: Int
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberPicker(
                    value = selectedHour,
                    onValueChange = { selectedHour = it },
                    range = 0..23
                )
                Text(":")
                NumberPicker(
                    value = selectedMinute,
                    onValueChange = { selectedMinute = it },
                    range = 0..59
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHour, selectedMinute) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = { if (value < range.last) onValueChange(value + 1) }
        ) {
            Text("▲")
        }
        Text(
            text = String.format("%02d", value),
            style = MaterialTheme.typography.headlineMedium
        )
        IconButton(
            onClick = { if (value > range.first) onValueChange(value - 1) }
        ) {
            Text("▼")
        }
    }
}
