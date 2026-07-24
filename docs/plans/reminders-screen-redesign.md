# RemindersScreen 重设计实现计划

## 设计原则

**提醒页 = 历史 + 管理**
- 历史视图：月度日历、本月统计
- 管理操作：按类型分组、批量操作

**与首页的区别**
- 首页：今日情况、实时状态
- 提醒页：历史记录、管理操作

## 页面结构

```
┌──────────────────────────────────────────┐
│ 健康提醒                          [+]    │
├──────────────────────────────────────────┤
│                                          │
│ ┌──────────────────────────────────────┐ │
│ │ 📅 2025年7月                         │ │
│ │ 日 一 二 三 四 五 六                 │ │
│ │    1  2  3  4  5  6                  │ │
│ │ 7  8  9 10 11 12 13                 │ │
│ │ 14 15 16 17 18 19 20                │ │
│ │ 21 22 23 [24] 25 26 27              │ │
│ │ ✅12天  ⚠️2天  📋3天                │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ ┌──────────────────────────────────────┐ │
│ │ 📊 本月统计                          │ │
│ │                                      │ │
│ │ 完成率 80%     连续服药 7天          │ │
│ │ ████████░░     🔥                   │ │
│ │                                      │ │
│ │ 总计 15项 · 完成 12项 · 逾期 2项    │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ ┌──────────────────────────────────────┐ │
│ │ 💊 服药提醒 (5项)              [管理]│ │
│ │                                      │ │
│ │ ☑ 服用降压药  每天    张三     [···] │ │
│ │ ☑ 服用降糖药  每天    李四     [···] │ │
│ │ ☐ 复查血糖    每周    张三     [···] │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ ┌──────────────────────────────────────┐ │
│ │ 🏥 复查提醒 (3项)              [管理]│ │
│ │                                      │ │
│ │ ☑ 年度体检    每年    王五     [···] │ │
│ │ ☐ 眼科复查    每半年  李四     [···] │ │
│ └──────────────────────────────────────┘ │
│                                          │
└──────────────────────────────────────────┘
```

## 文件变更清单

### 1. 修改文件

| 文件 | 改动内容 |
|------|----------|
| `RemindersScreen.kt` | 重写整个页面 |
| `RemindersViewModel.kt` | 添加月度统计数据 |

### 2. 无需修改
- `HealthTodo.kt` - 数据模型不变
- `HomeScreen.kt` - 不变

## 详细实现步骤

### Step 1: 更新 RemindersViewModel

添加月度统计计算：

```kotlin
data class RemindersUiState(
    val members: List<FamilyMember> = emptyList(),
    val todos: List<HealthTodo> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = false,
    // 新增
    val monthlyStats: MonthlyStats = MonthlyStats()
)

data class MonthlyStats(
    val total: Int = 0,
    val completed: Int = 0,
    val overdue: Int = 0,
    val completionRate: Float = 0f,
    val streak: Int = 0,
    val completedDays: Set<LocalDate> = emptySet(),
    val overdueDays: Set<LocalDate> = emptySet()
)
```

在 ViewModel 中添加计算方法：

```kotlin
private fun calculateMonthlyStats(todos: List<HealthTodo>): MonthlyStats {
    val today = LocalDate.now()
    val monthStart = today.withDayOfMonth(1)
    val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
    
    val monthTodos = todos.filter { it.dueDate in monthStart..monthEnd }
    val completed = monthTodos.count { it.done }
    val overdue = monthTodos.count { !it.done && it.dueDate.isBefore(today) }
    
    val completedDays = monthTodos.filter { it.done }.map { it.dueDate }.toSet()
    val overdueDays = monthTodos.filter { !it.done && it.dueDate.isBefore(today) }.map { it.dueDate }.toSet()
    
    // 计算连续天数
    val streak = calculateStreak(todos)
    
    return MonthlyStats(
        total = monthTodos.size,
        completed = completed,
        overdue = overdue,
        completionRate = if (monthTodos.isNotEmpty()) completed.toFloat() / monthTodos.size else 0f,
        streak = streak,
        completedDays = completedDays,
        overdueDays = overdueDays
    )
}

private fun calculateStreak(todos: List<HealthTodo>): Int {
    val today = LocalDate.now()
    var streak = 0
    var checkDate = today
    
    while (true) {
        val dayTodos = todos.filter { it.dueDate == checkDate }
        if (dayTodos.isEmpty()) break
        if (dayTodos.all { it.done }) {
            streak++
            checkDate = checkDate.minusDays(1)
        } else {
            break
        }
    }
    return streak
}
```

### Step 2: 重写 RemindersScreen

#### 主要组件

1. **MonthlyCalendar** - 月度日历
2. **MonthlyStatsCard** - 月度统计卡片
3. **ReminderTypeGroup** - 按类型分组的提醒列表
4. **ReminderItem** - 单个提醒项

#### 移除的组件

- `MemberFilterRow` - 成员筛选网格
- `StatusFilterRow` - 状态筛选芯片
- `MedicationCalendarHeader` - 旧的日历头部
- `MedicationCalendar` - 旧的日历组件

### Step 3: 实现月度日历

```kotlin
@Composable
private fun MonthlyCalendar(
    stats: MonthlyStats,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 月份标题
            Text(
                today.format(DateTimeFormatter.ofPattern("yyyy年M月")),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            
            // 星期头部
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日","一","二","三","四","五","六").forEach { d ->
                    Text(d, style = MaterialTheme.typography.labelSmall, 
                         color = MaterialTheme.colorScheme.onSurfaceVariant,
                         modifier = Modifier.weight(1f), 
                         textAlign = TextAlign.Center)
                }
            }
            
            // 日期网格
            val days = (1..today.lengthOfMonth()).map { today.withDayOfMonth(it) }
            days.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        val isCompleted = date in stats.completedDays
                        val isOverdue = date in stats.overdueDays
                        val isToday = date == today
                        val isSelected = date == selectedDate
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .padding(1.5.dp)
                                .clip(CircleShape)
                                .background(when {
                                    isCompleted -> Color(0xFF4CAF50)
                                    isOverdue -> MaterialTheme.colorScheme.error
                                    isSelected -> Primary.copy(alpha = 0.2f)
                                    isToday -> Primary.copy(alpha = 0.1f)
                                    else -> Color.Transparent
                                })
                                .clickable { onSelectDate(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${date.dayOfMonth}",
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isCompleted -> Color.White
                                    isOverdue -> Color.White
                                    isToday -> Primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
            
            // 图例
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendItem(color = Color(0xFF4CAF50), label = "✅${stats.completedDays.size}天")
                LegendItem(color = MaterialTheme.colorScheme.error, label = "⚠️${stats.overdueDays.size}天")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
```

### Step 4: 实现月度统计卡片

```kotlin
@Composable
private fun MonthlyStatsCard(stats: MonthlyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "本月统计",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 完成率
                Column {
                    Text(
                        "${(stats.completionRate * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Text("完成率", style = MaterialTheme.typography.labelSmall)
                }
                
                // 连续天数
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${stats.streak}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    Text("连续天数", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // 进度条
            LinearProgressIndicator(
                progress = { stats.completionRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Primary,
                trackColor = Primary.copy(alpha = 0.12f)
            )
            
            Spacer(Modifier.height(8.dp))
            
            // 统计详情
            Text(
                "总计 ${stats.total}项 · 完成 ${stats.completed}项 · 逾期 ${stats.overdue}项",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### Step 5: 实现按类型分组

```kotlin
@Composable
private fun ReminderTypeGroup(
    type: String,
    icon: String,
    todos: List<HealthTodo>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggle: (HealthTodo, Boolean) -> Unit,
    onDelete: (HealthTodo) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(12.dp)
        ) {
            // 头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    type,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${todos.size}项",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 展开内容
            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                todos.forEach { todo ->
                    ReminderItem(
                        todo = todo,
                        onToggle = { onToggle(todo, !todo.done) },
                        onDelete = { onDelete(todo) }
                    )
                }
            }
        }
    }
}
```

### Step 6: 提醒项组件

```kotlin
@Composable
private fun ReminderItem(
    todo: HealthTodo,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isOverdue = todo.dueDate.isBefore(LocalDate.now()) && !todo.done
    val repeatLabel = RemindersViewModel.repeatLabel(todo.repeatType, todo.repeatInterval)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.done,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = Primary)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                todo.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (todo.done) TextDecoration.LineThrough else null
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    todo.dueDate.format(DateTimeFormatter.ofPattern("MM-dd")),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverdue) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (repeatLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            repeatLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    todo.memberName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
```

## 需要添加的 imports

```kotlin
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.style.TextAlign
```

## 需要移除的 imports

```kotlin
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Remove
```

## 测试要点

1. **月度日历** - 验证日期显示正确，点击选择日期
2. **月度统计** - 验证完成率、连续天数计算正确
3. **按类型分组** - 验证服药/复查/检查分组正确
4. **展开/收起** - 验证分类展开收起功能
5. **提醒操作** - 验证完成、删除功能正常
6. **空状态** - 验证没有提醒时的显示

## 预计工作量

- 修改文件：2 个
- 新增代码：约 400 行
- 删除代码：约 300 行
- 测试时间：45 分钟

## 风险评估

- **中风险** - 涉及 ViewModel 数据结构改动
- **兼容性** - 需要保持现有数据结构不变
- **性能** - 月度统计计算需要优化，避免频繁计算