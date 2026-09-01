package com.yy.medtrace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yy.medtrace.R
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.ui.components.MemberAvatar
import com.yy.medtrace.ui.components.TodoCategory
import com.yy.medtrace.ui.components.todoCategoryFromStored
import com.yy.medtrace.ui.theme.memberCardColors
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 今日待办条目。
 */
@Composable
internal fun TodayTodoItem(
    todo: HealthTodo,
    member: FamilyMember?,
    onToggle: () -> Unit,
    isElderlyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val category = todoCategoryFromStored(todo.category)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.done,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(if (isElderlyMode) 32.dp else 24.dp)
        )
        if (member != null) {
            val (bg, content) = memberCardColors(member.relation, member.gender)
            MemberAvatar(
                member = member,
                size = if (isElderlyMode) 32.dp else 24.dp,
                fallbackBackground = bg,
                fallbackContent = content
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            category.icon,
            contentDescription = null,
            tint = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(if (isElderlyMode) 22.dp else 18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = todo.content,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = if (isElderlyMode) 18.sp else MaterialTheme.typography.bodyMedium.fontSize
            ),
            color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (todo.done) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 最近就诊记录条目。
 */
@Composable
internal fun RecentRecordItem(
    record: MedicalRecord,
    members: List<FamilyMember>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val member = members.find { it.id == record.patientId }
    // 相对时间需要定时器驱动，否则会停在打开页面的那一刻
    val timeAgo = rememberTimeAgo(record.onsetTime)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (member != null) {
            val (bg, content) = memberCardColors(member.relation, member.gender)
            MemberAvatar(
                member = member,
                size = 36.dp,
                fallbackBackground = bg,
                fallbackContent = content
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MedicalInformation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                record.diagnosis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    timeAgo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (member != null) {
                    Text(
                        "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        member.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 每 [periodMillis] 触发一次重组的心跳，用于让"x 分钟前"这类相对时间自动刷新。
 */
@Composable
fun rememberAutoRefreshTick(periodMillis: Long = 60_000L): State<Long> {
    val tick = remember(periodMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMillis) {
        while (true) {
            delay(periodMillis)
            tick.longValue = System.currentTimeMillis()
        }
    }
    return tick
}

@Composable
fun rememberTimeAgo(dateTime: LocalDateTime): String {
    // 以心跳时间作为基准，读取 state 即建立重组依赖，从而驱动相对时间自动刷新
    val now = java.time.Instant.ofEpochMilli(rememberAutoRefreshTick().value)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
    val minutes = java.time.Duration.between(dateTime, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.screen_home_just_now)
        minutes < 60 -> stringResource(R.string.screen_home_minutes_ago, minutes.toInt())
        minutes < 1440 -> stringResource(R.string.screen_home_hours_ago, (minutes / 60).toInt())
        minutes < 10080 -> stringResource(R.string.screen_home_days_ago, (minutes / 1440).toInt())
        else -> dateTime.format(DateTimeFormatter.ofPattern("M月d日"))
    }
}
