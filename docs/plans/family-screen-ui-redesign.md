# FamilyScreen UI 重设计实现计划

## 目标
优化家庭管理页面的成员列表 UI，采用精简卡片 + 点击展开的设计模式。

## 设计原则
1. **精简默认状态** - 收起时只显示核心信息
2. **按需展开** - 点击展开查看详细信息
3. **视觉层级清晰** - 健康标签用颜色区分
4. **操作按钮合理** - 常用操作易达，危险操作隐藏

## 文件变更清单

### 1. 修改文件
- `app/src/main/java/com/yy/medtrace/ui/screens/FamilyScreen.kt`
  - 重构 `MemberCard` 组件
  - 添加展开/收起状态管理
  - 优化布局结构

- `app/src/main/java/com/yy/medtrace/ui/theme/Color.kt`
  - 添加健康标签颜色定义

### 2. 无需修改
- `MemberAvatar.kt` - 保持现有头像组件
- `MemberStyle.kt` - 保持现有成员配色
- `HealthDashboard` - 保持现有概览卡片

## 详细实现步骤

### Step 1: 添加健康标签颜色 (Color.kt)

```kotlin
// 健康标签颜色
object HealthTagColors {
    val AllergyBg = Color(0xFFFFEBEE)      // 浅红
    val AllergyContent = Color(0xFFD32F2F)  // 红
    val ChronicBg = Color(0xFFFFF3E0)       // 浅橙
    val ChronicContent = Color(0xFFF57C00)  // 橙
    val MedicationBg = Color(0xFFE3F2FD)    // 浅蓝
    val MedicationContent = Color(0xFF1976D2) // 蓝
    val DefaultBg = Color(0xFFF5F5F5)       // 浅灰
    val DefaultContent = Color(0xFF757575)   // 灰
}
```

### Step 2: 重构 MemberCard 组件

#### 收起状态（默认）
```
┌──────────────────────────────────────────┐
│ [头像] 张三  本人 · 25岁                │
│    [过敏: 青霉素] [糖尿病]         [▶]  │
└──────────────────────────────────────────┘
```

#### 展开状态
```
┌──────────────────────────────────────────┐
│ [头像] 张三 "默认"                       │
│        本人 · 25岁 · 男 · A型            │
│                                          │
│ ┌────────────────────────────────────┐   │
│ │ 过敏   青霉素、花粉                │   │
│ │ 慢性病 糖尿病                       │   │
│ │ 用药   二甲双胍                     │   │
│ └────────────────────────────────────┘   │
│                                          │
│ 最近就诊                                 │
│ 01-15 感冒  ·  01-10 咳嗽               │
│                                          │
│ 记录 5  ·  待办 2  ·  逾期 1            │
│                                          │
│ [编辑] [添加记录] [查看病历] [▼]         │
└──────────────────────────────────────────┘
```

### Step 3: 实现展开/收起状态管理

```kotlin
// 在 FamilyScreen 中管理展开状态
var expandedMemberId by remember { mutableStateOf<Long?>(null) }

// 传递给 MemberCard
MemberCard(
    member = member,
    isExpanded = expandedMemberId == member.id,
    onToggleExpand = {
        expandedMemberId = if (expandedMemberId == member.id) null else member.id
    },
    // ... 其他参数
)
```

### Step 4: 重构布局结构

```kotlin
@Composable
private fun MemberCard(
    member: FamilyMember,
    isExpanded: Boolean,
    recordCount: Int,
    recentRecords: List<MedicalRecord>,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddRecord: () -> Unit,
    onViewRecords: () -> Unit
) {
    val (cardBg, cardContent) = memberCardColors(member.relation, member.gender)
    val age = computeAge(member.birthday)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 头部：头像 + 基本信息 + 展开按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                MemberAvatar(
                    member = member,
                    size = if (isExpanded) 56.dp else 44.dp,
                    fallbackBackground = cardContent.copy(alpha = 0.18f),
                    fallbackContent = cardContent
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // 姓名 + 默认标签
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            member.name,
                            style = if (isExpanded) MaterialTheme.typography.titleMedium
                                   else MaterialTheme.typography.bodyLarge,
                            color = cardContent,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (member.isDefault) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = AppShapes.small,
                                color = cardContent.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    "默认",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cardContent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    // 关系 · 年龄（收起状态）
                    if (!isExpanded) {
                        val sub = buildList {
                            if (member.relation.isNotBlank()) add(member.relation)
                            age?.let { add("${it}岁") }
                        }.joinToString(" · ")
                        if (sub.isNotBlank()) {
                            Text(
                                sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = cardContent.copy(alpha = 0.8f)
                            )
                        }
                    }
                    // 关系 · 年龄 · 性别 · 血型（展开状态）
                    if (isExpanded) {
                        val sub = buildList {
                            if (member.relation.isNotBlank()) add(member.relation)
                            age?.let { add("${it}岁") }
                            if (member.gender.isNotBlank()) add(member.gender)
                            if (member.bloodType.isNotBlank()) add("${member.bloodType}型")
                        }.joinToString(" · ")
                        if (sub.isNotBlank()) {
                            Text(
                                sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = cardContent.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                // 展开/收起按钮
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess
                                     else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = cardContent.copy(alpha = 0.6f)
                    )
                }
            }

            // 健康标签（收起状态）
            if (!isExpanded) {
                val healthTags = buildList {
                    member.allergy.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        .forEach { add("过敏: $it") }
                    member.chronic.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        .forEach { add(it) }
                }
                if (healthTags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        healthTags.forEach { tag ->
                            val isAllergy = tag.startsWith("过敏:")
                            Surface(
                                shape = AppShapes.small,
                                color = if (isAllergy) HealthTagColors.AllergyBg
                                       else HealthTagColors.ChronicBg
                            ) {
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAllergy) HealthTagColors.AllergyContent
                                           else HealthTagColors.ChronicContent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 展开状态的内容
            if (isExpanded) {
                // 健康详情表格
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (member.allergy.isNotBlank()) {
                        HealthInfoRow(label = "过敏", value = member.allergy, color = cardContent)
                    }
                    if (member.chronic.isNotBlank()) {
                        HealthInfoRow(label = "慢性病", value = member.chronic, color = cardContent)
                    }
                    if (member.medicationNote.isNotBlank()) {
                        HealthInfoRow(label = "用药", value = member.medicationNote, color = cardContent)
                    }
                }

                // 最近就诊记录
                if (recentRecords.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "最近就诊",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cardContent
                        )
                        recentRecords.forEach { record ->
                            Text(
                                "${record.onsetTime.format(DateTimeFormatter.ofPattern("MM-dd"))} ${record.diagnosis}",
                                style = MaterialTheme.typography.labelSmall,
                                color = cardContent.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 统计信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(value = "$recordCount", label = "记录", color = cardContent)
                }

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        shape = AppShapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cardContent)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("编辑", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onAddRecord,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        shape = AppShapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cardContent)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("添加记录", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onViewRecords,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        shape = AppShapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cardContent)
                    ) {
                        Text("查看病历", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // 删除按钮
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    shape = AppShapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("删除成员", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
```

### Step 5: 添加辅助组件

```kotlin
@Composable
private fun HealthInfoRow(
    label: String,
    value: String,
    color: Color
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.width(64.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f)
        )
    }
}
```

## 需要添加的 imports

```kotlin
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.layout.ExperimentalLayoutApi
```

## 测试要点

1. **收起状态** - 验证卡片显示正确
2. **展开状态** - 验证详情显示正确
3. **点击切换** - 验证展开/收起动画
4. **健康标签** - 验证颜色显示正确
5. **操作按钮** - 验证所有按钮功能正常
6. **空状态** - 验证没有健康信息时的显示

## 预计工作量

- 修改文件：2 个
- 新增代码：约 200 行
- 删除代码：约 100 行
- 测试时间：30 分钟

## 风险评估

- **低风险** - 只涉及 UI 层改动，不影响业务逻辑
- **兼容性** - 保持现有数据结构不变
- **性能** - 使用 `remember` 优化展开/收起状态