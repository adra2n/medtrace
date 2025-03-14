package com.yy.chiyaole.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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

    // 处理设置跳转
    fun handleSettingsNavigation(intent: Intent, settingName: String) {
        try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(
                    context,
                    "无法打开${settingName}设置，请手动前往系统设置",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "打开设置失败：${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            reminderPreviewUtil.release()
        }
    }

    LaunchedEffect(Unit) {
        database.userSettingsDao().getUserSettings().collect { userSettings ->
            settings = userSettings ?: UserSettings()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "设置",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 权限设置
                item {
                    SettingsSection(title = "权限设置") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SettingsItem(
                                title = "通知权限",
                                description = "请确保已授予通知权限",
                                icon = Icons.Outlined.Notifications,
                                onClick = {
                                    val intent = Intent().apply {
                                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    handleSettingsNavigation(intent, "通知")
                                }
                            )

                            SettingsItem(
                                title = "自动运行",
                                description = "允许应用在后台运行",
                                icon = Icons.Filled.Settings,
                                onClick = {
                                    val intent = Intent().apply {
                                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    handleSettingsNavigation(intent, "自动化")
                                }
                            )

                            SettingsItem(
                                title = "电池优化",
                                description = "关闭电池优化以保证提醒",
                                icon = Icons.Filled.Check,
                                onClick = {
                                    val intent = Intent().apply {
                                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    handleSettingsNavigation(intent, "电池优化")
                                }
                            )
                        }
                    }
                }

                // 提醒设置
                item {
                    SettingsSection(title = "提醒设置") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "提前提醒",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        "${settings?.reminderAdvanceMinutes ?: 30}分钟",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Slider(
                                    value = (settings?.reminderAdvanceMinutes ?: 30).toFloat(),
                                    onValueChange = { minutes ->
                                        scope.launch {
                                            val updatedSettings = settings?.copy(
                                                reminderAdvanceMinutes = minutes.toInt()
                                            ) ?: UserSettings(reminderAdvanceMinutes = minutes.toInt())
                                            database.userSettingsDao().insertOrUpdate(updatedSettings)
                                        }
                                    },
                                    valueRange = 1f..30f,
                                    steps = 23,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                )
                            }

                            // 语音提醒设置
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "语音提醒",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Switch(
                                        checked = settings?.enableVoiceReminder ?: false,
                                        onCheckedChange = { isChecked ->
                                            scope.launch {
                                                val updatedSettings = settings?.copy(
                                                    enableVoiceReminder = isChecked
                                                ) ?: UserSettings(enableVoiceReminder = isChecked)
                                                database.userSettingsDao().insertOrUpdate(updatedSettings)
                                            }
                                            if (isChecked) {
                                                reminderPreviewUtil.previewVoiceReminder()
                                            }
                                        }
                                    )
                                }
                            }

                            // 通知声音设置
                            // Row(
                            //     modifier = Modifier.fillMaxWidth(),
                            //     horizontalArrangement = Arrangement.SpaceBetween,
                            //     verticalAlignment = Alignment.CenterVertically
                            // ) {
                            //     Text(
                            //         "通知声音",
                            //         style = MaterialTheme.typography.titleSmall
                            //     )
                            //     Switch(
                            //         checked = settings?.enableNotificationSound ?: false,
                            //         onCheckedChange = { isChecked ->
                            //             scope.launch {
                            //                 val updatedSettings = settings?.copy(
                            //                     enableNotificationSound = isChecked
                            //                 ) ?: UserSettings(enableNotificationSound = isChecked)
                            //                 database.userSettingsDao().insertOrUpdate(updatedSettings)
                            //             }
                            //             if (isChecked) {
                            //                 reminderPreviewUtil.previewNotificationSound()
                            //             }
                            //         }
                            //     )
                            // }

                            // 震动设置
                            // Row(
                            //     modifier = Modifier.fillMaxWidth(),
                            //     horizontalArrangement = Arrangement.SpaceBetween,
                            //     verticalAlignment = Alignment.CenterVertically
                            // ) {
                            //     Text(
                            //         "震动",
                            //         style = MaterialTheme.typography.titleSmall
                            //     )
                            //     Switch(
                            //         checked = settings?.enableVibration ?: false,
                            //         onCheckedChange = { isChecked ->
                            //             scope.launch {
                            //                 val updatedSettings = settings?.copy(
                            //                     enableVibration = isChecked
                            //                 ) ?: UserSettings(enableVibration = isChecked)
                            //                 database.userSettingsDao().insertOrUpdate(updatedSettings)
                            //             }
                            //             if (isChecked) {
                            //                 reminderPreviewUtil.previewVibration()
                            //             }
                            //         }
                            //     )
                            // }
                        }
                    }
                }

                // 外观设置
                item {
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
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    "完成",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )

    if (showAdvanceTimeDialog) {
        AlertDialog(
            onDismissRequest = { showAdvanceTimeDialog = false },
            title = { Text("提前提醒（分钟）") },
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
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "进入设置",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = { if (value > range.first) onValueChange(value - 1) },
            enabled = value > range.first
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, "减少")
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.widthIn(min = 48.dp),
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = { if (value < range.last) onValueChange(value + 1) },
            enabled = value < range.last
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, "增加")
        }
    }
}
