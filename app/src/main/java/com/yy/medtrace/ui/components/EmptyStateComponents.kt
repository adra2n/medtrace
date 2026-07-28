package com.yy.medtrace.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yy.medtrace.ui.theme.Primary

/**
 * 空状态组件
 * 用于页面无数据时展示
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    hint: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标背景
        Surface(
            shape = CircleShape,
            color = Primary.copy(alpha = 0.1f),
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Primary.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 标题
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        // 提示
        if (hint != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        // 操作按钮
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionText)
            }
        }
    }
}

/**
 * 首页空状态
 */
@Composable
fun EmptyHomeState(onAdd: () -> Unit) {
    EmptyState(
        icon = Icons.Default.EventNote,
        title = "今天暂无就诊计划",
        hint = "点击下方按钮添加第一条记录",
        actionText = "立即添加",
        onAction = onAdd
    )
}

/**
 * 家庭管理空状态
 */
@Composable
fun EmptyFamilyState(onAdd: () -> Unit) {
    EmptyState(
        icon = Icons.Default.People,
        title = "还没有家庭成员",
        hint = "点击右上角按钮添加你的第一位家人",
        actionText = "添加成员",
        onAction = onAdd
    )
}

/**
 * 数据统计空状态
 */
@Composable
fun EmptyStatsState() {
    EmptyState(
        icon = Icons.Default.ShowChart,
        title = "暂无数据统计",
        hint = "添加就诊记录后，这里会显示数据统计"
    )
}

/**
 * 提醒管理空状态
 */
@Composable
fun EmptyReminderState(onAdd: () -> Unit) {
    EmptyState(
        icon = Icons.Default.Notifications,
        title = "暂无提醒事项",
        hint = "点击右上角按钮添加用药、复查等提醒",
        actionText = "添加提醒",
        onAction = onAdd
    )
}

/**
 * 搜索结果空状态
 */
@Composable
fun EmptySearchState() {
    EmptyState(
        icon = Icons.Default.Search,
        title = "没有找到相关记录",
        hint = "试试其他关键词"
    )
}

/**
 * 医疗记录空状态
 */
@Composable
fun EmptyRecordsState(onAdd: (() -> Unit)? = null) {
    EmptyState(
        icon = Icons.Default.EventNote,
        title = "暂无就诊记录",
        hint = "点击右下角按钮添加第一条记录",
        actionText = if (onAdd != null) "添加记录" else null,
        onAction = onAdd
    )
}
