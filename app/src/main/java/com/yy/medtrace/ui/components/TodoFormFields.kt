package com.yy.medtrace.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yy.medtrace.R
import com.yy.medtrace.data.model.FamilyMember
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 待办分类。
 *
 * 首页与提醒页此前各自维护一套「文案 + emoji」映射，两处容易漂移，
 * 且 emoji 在不同机型 / 深色模式下渲染不一致。这里统一为 Material 图标。
 * 数据库中仍以本地化文案存储（兼容既有数据），读写时通过 [fromStored] / [todoCategoryLabel] 转换。
 */
enum class TodoCategory(
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    MEDICATION(R.string.todo_category_medication, Icons.Filled.Medication),
    FOLLOWUP(R.string.todo_category_followup, Icons.Filled.LocalHospital),
    CHECKUP(R.string.todo_category_checkup, Icons.Filled.Science),
    OTHER(R.string.todo_category_other, Icons.Filled.Description);

    companion object {
        /** 按已存储的本地化文案反查分类；匹配不到时回退到「其他」。 */
        fun fromStored(value: String, labels: Map<TodoCategory, String>): TodoCategory =
            entries.firstOrNull { labels[it] == value } ?: OTHER

        fun fromStored(value: String, resolve: (TodoCategory) -> String): TodoCategory =
            entries.firstOrNull { resolve(it) == value } ?: OTHER
    }
}

/** 全部分类及其当前语言的文案。 */
@Composable
@ReadOnlyComposable
fun todoCategoryLabels(): Map<TodoCategory, String> =
    TodoCategory.entries.associateWith { stringResource(it.labelRes) }

@Composable
@ReadOnlyComposable
fun todoCategoryLabel(category: TodoCategory): String = stringResource(category.labelRes)

/** 从数据库存储的本地化文案还原分类（需 Compose 上下文取文案）。 */
@Composable
@ReadOnlyComposable
fun todoCategoryFromStored(value: String): TodoCategory =
    TodoCategory.entries.firstOrNull { stringResource(it.labelRes) == value } ?: TodoCategory.OTHER

/**
 * 重复周期。storageKey 与数据库 / [com.yy.medtrace.viewmodel.RemindersViewModel.calculateNextDueDate]
 * 使用的字符串保持一致。模型层早已支持 month / year，此前只是 UI 没有暴露。
 */
enum class RepeatType(@StringRes val labelRes: Int) {
    NONE(R.string.todo_repeat_none),
    DAY(R.string.todo_repeat_day),
    WEEK(R.string.todo_repeat_week),
    MONTH(R.string.todo_repeat_month),
    YEAR(R.string.todo_repeat_year);

    val storageKey: String get() = name.lowercase()

    companion object {
        fun fromKey(key: String?): RepeatType =
            entries.firstOrNull { it.storageKey == key?.lowercase() } ?: NONE

        /** 可选的重复项（不含「不重复」），用于选择器。 */
        val selectable: List<RepeatType> get() = entries.filter { it != NONE }
    }
}

@Composable
@ReadOnlyComposable
fun repeatLabel(type: RepeatType): String = stringResource(type.labelRes)

/** 列表里展示的重复标签；不重复时返回 null（不展示徽标）。 */
@Composable
@ReadOnlyComposable
fun repeatLabelOrNull(type: RepeatType): String? =
    if (type == RepeatType.NONE) null else stringResource(type.labelRes)

/**
 * 分类四选一。两行两列，图标 + 文案。
 */
@Composable
fun CategorySelector(
    selected: TodoCategory,
    onSelect: (TodoCategory) -> Unit,
    modifier: Modifier = Modifier,
    isElderlyMode: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TodoCategory.entries.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { category ->
                    FilterChip(
                        selected = selected == category,
                        onClick = { onSelect(category) },
                        label = { Text(todoCategoryLabel(category)) },
                        leadingIcon = {
                            Icon(
                                category.icon,
                                contentDescription = null,
                                modifier = Modifier.size(if (isElderlyMode) 20.dp else 16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 只读文本框的统一样式（禁用态下仍保持可读，避免"灰色不可点"的错觉）。
 */
@Composable
private fun readOnlyFieldColors() = OutlinedTextFieldDefaults.colors(
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
    disabledBorderColor = MaterialTheme.colorScheme.outline
)

/** 计划日期：只读文本框 + DatePicker 弹窗。 */
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReminderDateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val todayLabel = stringResource(R.string.todo_date_today)
    val dateLabel = remember(date, todayLabel) {
        if (date == LocalDate.now()) todayLabel
        else date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    OutlinedTextField(
        value = dateLabel,
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(stringResource(R.string.todo_label_date)) },
        trailingIcon = {
            Icon(
                Icons.Filled.DateRange,
                contentDescription = stringResource(R.string.todo_cd_select_date),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true },
        colors = readOnlyFieldColors()
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateChange(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            )
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.screen_home_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.screen_home_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** 提醒时间：只读文本框 + TimePicker 弹窗（24 小时制）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimeField(
    time: String,
    onTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = time,
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(stringResource(R.string.todo_label_reminder_time)) },
        trailingIcon = {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = stringResource(R.string.todo_cd_select_time),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showTimePicker = true },
        colors = readOnlyFieldColors()
    )

    if (showTimePicker) {
        val parts = time.split(":")
        val timePickerState = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.screen_home_select_reminder_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hour = timePickerState.hour.toString().padStart(2, '0')
                        val minute = timePickerState.minute.toString().padStart(2, '0')
                        onTimeChange("$hour:$minute")
                        showTimePicker = false
                    }
                ) { Text(stringResource(R.string.screen_home_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.screen_home_cancel))
                }
            }
        )
    }
}

/** 重复设置：只读文本框 + 重复方式弹窗。 */
@Composable
fun RepeatField(
    repeat: RepeatType,
    onRepeatChange: (RepeatType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = repeatLabel(repeat),
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(stringResource(R.string.todo_repeat_label)) },
        trailingIcon = {
            Icon(
                Icons.Filled.Repeat,
                contentDescription = stringResource(R.string.todo_cd_select_repeat),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        colors = readOnlyFieldColors()
    )

    if (showDialog) {
        RepeatPickerDialog(
            current = repeat,
            onConfirm = {
                onRepeatChange(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/** 重复方式选择弹窗：不重复 / 每天 / 每周 / 每月 / 每年。 */
@Composable
fun RepeatPickerDialog(
    current: RepeatType,
    onConfirm: (RepeatType) -> Unit,
    onDismiss: () -> Unit
) {
    var repeatEnabled by remember { mutableStateOf(current != RepeatType.NONE) }
    var selected by remember {
        mutableStateOf(if (current == RepeatType.NONE) RepeatType.DAY else current)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.todo_repeat_select_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RepeatChip(
                        label = stringResource(R.string.todo_repeat_none),
                        selected = !repeatEnabled,
                        onClick = { repeatEnabled = false },
                        modifier = Modifier.weight(1f)
                    )
                    RepeatChip(
                        label = stringResource(R.string.todo_repeat_enabled),
                        selected = repeatEnabled,
                        onClick = { repeatEnabled = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (repeatEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RepeatType.selectable.chunked(2).forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowItems.forEach { type ->
                                    RepeatChip(
                                        label = repeatLabel(type),
                                        selected = selected == type,
                                        onClick = { selected = type },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(if (repeatEnabled) selected else RepeatType.NONE) }) {
                Text(stringResource(R.string.screen_home_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.screen_home_cancel)) }
        }
    )
}

@Composable
private fun RepeatChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    )
}

/**
 * 成员下拉选择。首页添加待办与提醒页共用同一套交互与文案。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberSelector(
    members: List<FamilyMember>,
    selectedId: Long?,
    onSelect: (FamilyMember) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = members.firstOrNull { it.id == selectedId }
    val memberLabel = stringResource(R.string.screen_home_member_format)
    val defaultRelation = stringResource(R.string.screen_home_member_default)

    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.let {
                memberLabel.format(it.name, it.relation.ifBlank { defaultRelation })
            } ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(stringResource(R.string.todo_label_member)) },
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.todo_cd_select_member),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = readOnlyFieldColors()
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            members.forEach { member ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            memberLabel.format(
                                member.name,
                                member.relation.ifBlank { defaultRelation }
                            )
                        )
                    },
                    onClick = {
                        onSelect(member)
                        expanded = false
                    }
                )
            }
        }
    }
}
