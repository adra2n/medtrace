# 医迹 (MedTrace) UI 设计审计报告

> 审计日期：2026-07-30 | 版本：v3.8.1 (versionCode 30)

---

## 一、审计范围

- `app/src/main/java/com/yy/medtrace/ui/theme/` — 主题、配色、字体、形状定义
- `app/src/main/java/com/yy/medtrace/ui/screens/` — 全部 14 个 Screen
- `app/src/main/java/com/yy/medtrace/ui/components/` — 全部公共组件

---

## 二、问题清单（共 25 项）

### 高严重度（8 项）

#### H1. GradientTopBar 深色模式完全失效

**文件**：`Design.kt:56,76,83`

GradientTopBar 硬编码 `.background(Color.White)`，标题用 `TextPrimary`（深灰 `#2E3A46`），副标题用 `TextSecondary`（灰 `#5F6B7A`）。深色模式下出现白色顶栏 + 深灰文字，完全不可读。

```kotlin
// Design.kt:56
.background(Color.White) // ← 应为 MaterialTheme.colorScheme.surface
// Design.kt:76
color = TextPrimary      // ← 应为 MaterialTheme.colorScheme.onSurface
// Design.kt:83
color = TextSecondary    // ← 应为 MaterialTheme.colorScheme.onSurfaceVariant
```

**影响**：所有使用 `GradientTopBar` 的页面（HomeScreen、FamilyScreen、RemindersScreen、ProfileScreen、MedicalRecordScreen、MemberDetailScreen、SettingsScreen、TrendsScreen、AddMedicalRecordScreen）在深色模式下顶栏不可用。

**修复方案**：将 `Color.White` → `MaterialTheme.colorScheme.surface`，`TextPrimary` → `MaterialTheme.colorScheme.onSurface`，`TextSecondary` → `MaterialTheme.colorScheme.onSurfaceVariant`。

---

#### H2. MedicalRecordScreen 顶栏 Add 图标不可见

**文件**：`MedicalRecordScreen.kt:167`

GradientTopBar 背景是 `Color.White`，但 Add 图标 `tint = Color.White`，白底白图标，浅色模式下完全不可见。

```kotlin
Icon(Icons.Default.Add, ..., tint = androidx.compose.ui.graphics.Color.White)
// ← 应为 MaterialTheme.colorScheme.onSurface 或 Primary
```

**修复方案**：将 `tint` 改为 `MaterialTheme.colorScheme.onSurface` 或 `Primary`。

---

#### H3. SettingsScreen 保存按钮深色模式失效

**文件**：`SettingsScreen.kt:353-354`

保存按钮 `containerColor = Color.White, contentColor = Primary`。深色模式下白色按钮 + 青色文字对比度极差。

```kotlin
containerColor = Color.White,  // ← 应为 MaterialTheme.colorScheme.onPrimary
contentColor = Primary         // ← 应为 MaterialTheme.colorScheme.primary
```

**修复方案**：使用 `MaterialTheme.colorScheme.onPrimary` / `primary`。

---

#### H4. SettingsScreen Switch 硬编码 Color.White

**文件**：`SettingsScreen.kt:395,397,444,446,487,489`

6 处 Switch 硬编码 `checkedThumbColor = Color.White` 和 `uncheckedThumbColor = Color.White`，绕过 MaterialTheme 语义色系统。

**修复方案**：移除自定义 `colors` 参数，使用 Switch 默认配色；或使用 `MaterialTheme.colorScheme` 语义色。

---

#### H5. ProfileScreen Premium 卡片硬编码 Color.White

**文件**：`ProfileScreen.kt:159,166,175,181,187`

Premium 卡片 `containerColor = Primary`，内部文字/图标全用 `Color.White`。应使用 `MaterialTheme.colorScheme.onPrimary` 保持语义一致性。

**修复方案**：将 `Color.White` → `MaterialTheme.colorScheme.onPrimary`。

---

#### H6. OnboardingScreen 全局 Color.White

**文件**：`OnboardingScreen.kt:67,74,76,79,89`

OnboardingScreen 使用 `PrimaryGradient` 作为全屏背景，文字/图标用 `Color.White`。虽然渐变背景不变，但应使用 `MaterialTheme.colorScheme.onPrimary` 保持一致性。

**修复方案**：将 `Color.White` → `MaterialTheme.colorScheme.onPrimary`。

---

#### H7. 触摸目标 < 48dp — 编辑/删除按钮

**文件**：`MedicalRecordScreen.kt:672,682`、`AddMedicalRecordScreen.kt:460`

```kotlin
modifier = Modifier.height(32.dp)  // Edit button
modifier = Modifier.height(32.dp)  // Delete button
```

三个 TextButton 高度仅 32dp，低于 Material Design 推荐的 48dp 最小触摸目标。

**修复方案**：移除 `height(32.dp)` 限制，或改为 `height(48.dp)`，或使用 `defaultMinSize(minHeight = 48.dp)`。

---

#### H8. FamilyScreen 删除按钮触摸目标不足

**文件**：`FamilyScreen.kt:446-454`

删除 TextButton `contentPadding = PaddingValues(vertical = 4.dp)`，内部图标仅 14dp，整体高度远低于 48dp。

**修复方案**：增大 `contentPadding` 至 `PaddingValues(vertical = 12.dp)`，图标尺寸至少 18dp。

---

### 中严重度（11 项）

#### M1. 页面水平边距不一致 — HomeScreen 独用 20dp

**文件**：`HomeScreen.kt:118`

| Screen | 水平 padding |
|--------|-------------|
| HomeScreen | **20.dp** |
| 其他所有 Screen | 16.dp |

**修复方案**：统一为 16.dp。

---

#### M2. Card 内部 padding 混乱

| 位置 | Card 内部 padding |
|------|-------------------|
| HomeScreen 家人/待办/记录卡片 | 14.dp |
| ProfileScreen 所有卡片 | 16.dp |
| MedicalRecordScreen 卡片 | 16.dp |
| MemberDetailScreen 指标卡 | 14.dp |
| RemindersScreen 月度统计卡 | 16.dp |
| RemindersScreen 类型分组卡 | 12.dp |
| FamilyScreen 成员卡 | 12.dp |
| FamilyScreen 健康概览卡 | 16.dp |
| SectionCard（组件） | 20.dp |
| TrendsScreen 记录卡 | 14.dp |

存在 12/14/16/20dp 四种 Card 内部 padding，无统一规范。

**修复方案**：统一为 16.dp。

---

#### M3. LazyColumn 间距不一致

| Screen | verticalArrangement |
|--------|-------------------|
| HomeScreen | **spacedBy(20.dp)** |
| RemindersScreen | **spacedBy(12.dp)** |
| MedicalRecordScreen | spacedBy(16.dp) |
| TrendsScreen | spacedBy(16.dp) |
| MemberDetailScreen | spacedBy(16.dp) |

**修复方案**：统一为 `spacedBy(16.dp)`。

---

#### M4. Card elevation 不一致

**文件**：`MedicalRecordCard.kt:46`、`HealthTipsCard.kt:83`

绝大多数 Card 使用 `SoftElevation`（8.dp），但这两个组件用 `defaultElevation = 1.dp`。

**修复方案**：统一使用 `SoftElevation`（8.dp）。

---

#### M5. Card shape 不一致

**文件**：`MemberDetailScreen.kt:192`、`TrendsScreen.kt:229`

大多数 Card 使用 `AppShapes.large`（20dp 圆角），但这两处用 `AppShapes.medium`（16dp）。

**修复方案**：统一使用 `AppShapes.large`。

---

#### M6. PremiumScreen Card 完全使用默认样式

**文件**：`PremiumScreen.kt:93,113,152`

三个 Card 未指定 `shape`、`colors`、`elevation`，使用 Material3 默认值，与项目其他 Card 的 `AppShapes.large` + `cardContainerColor()` + `SoftElevation` 规范完全不同。

**修复方案**：补全 `shape = AppShapes.large`、`colors = CardDefaults.cardColors(containerColor = cardContainerColor())`、`elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)`。

---

#### M7. 大量直接引用 Color.kt 顶层变量，绕过 MaterialTheme

**文件**：几乎所有 Screen

| 文件 | 直接引用的 Color.kt 变量 |
|------|------------------------|
| HomeScreen.kt | Primary, Info, Healthy, Reminder, NoStatus, Urgent |
| ProfileScreen.kt | Primary |
| SettingsScreen.kt | Primary |
| RemindersScreen.kt | Primary, Reminder |
| TrendsScreen.kt | Primary |
| FamilyScreen.kt | Primary, Reminder, NoStatus, Healthy |
| AddMedicalRecordScreen.kt | Primary |
| LockScreen.kt | Primary |
| EmptyStateComponents.kt | Primary |
| Design.kt | Primary, TextPrimary, TextSecondary |

这些颜色在深色模式下不会自动切换，导致深色模式下出现浅色模式的色调。

**修复方案**：将 `Primary` → `MaterialTheme.colorScheme.primary`，`TextPrimary` → `MaterialTheme.colorScheme.onSurface`，`TextSecondary` → `MaterialTheme.colorScheme.onSurfaceVariant`。功能色（Info/Healthy/Reminder 等）可保留直接引用（因为它们是语义固定的状态色，不随明暗切换），但需确认在深色模式下对比度足够。

---

#### M8. Design.kt SectionTitle 硬编码 Primary

**文件**：`Design.kt:101`

```kotlin
color = Primary, // 青绿色
```

应使用 `MaterialTheme.colorScheme.primary`。

---

#### M9. Color.kt caption 扩展属性不适配深色模式

**文件**：`Color.kt:80-81`

```kotlin
val androidx.compose.material3.ColorScheme.caption
    get() = TextDisabled
```

无论明暗模式都返回 `TextDisabled`（浅灰 `#98A2B3`），深色模式下该颜色在深色背景上对比度不足。

**修复方案**：改为条件返回：

```kotlin
val androidx.compose.material3.ColorScheme.caption
    @Composable get() = if (LocalIsDark.current) Color(0xFF8B949E) else TextDisabled
```

---

#### M10. MemberColors / HealthTagColors 全部为浅色模式硬编码

**文件**：`Color.kt:56-77`

`MemberColors` 和 `HealthTagColors` 对象中的颜色全部是浅色模式专用色，在深色模式下作为 Card 背景会导致浅色卡片浮在深色背景上。

**修复方案**：改为 `@Composable` 函数，根据 `LocalIsDark.current` 返回不同色值；或在使用处加 `.copy(alpha = 0.15f)` 降低对比度。

---

#### M11. 硬编码 fontSize / fontWeight

**文件**：`OnboardingScreen.kt`、`SplashScreen.kt`、`LockScreen.kt`、`PrivacyConsentScreen.kt`、`PrivacyPolicyScreen.kt`、`UserAgreementScreen.kt`

| 文件:行 | 硬编码值 | 应使用 |
|---------|---------|--------|
| OnboardingScreen.kt:76 | `fontSize = 26.sp` | `titleLarge` (28sp) |
| OnboardingScreen.kt:80 | `fontSize = 16.sp` | `bodyLarge` |
| OnboardingScreen.kt:77 | `fontWeight = FontWeight.Bold` | typography style |
| SplashScreen.kt:80 | `fontSize = 32.sp` | `displaySmall` |
| SplashScreen.kt:81 | `fontWeight = FontWeight.Bold` | typography style |
| SplashScreen.kt:90 | `fontSize = 16.sp` | `bodyLarge` |
| LockScreen.kt:205 | `fontSize = 28.sp` | `titleLarge` |
| LockScreen.kt:206 | `fontWeight = FontWeight.Medium` | typography style |
| PrivacyConsentScreen.kt:58 | `fontSize = 22.sp` | `titleMedium` |
| PrivacyConsentScreen.kt:70,79,96 | `fontSize = 15.sp` | `labelLarge` |
| PrivacyConsentScreen.kt:86 | `fontSize = 14.sp` | `bodyMedium` |
| PrivacyConsentScreen.kt:108,115 | `fontSize = 16.sp` | `bodyLarge` |
| PrivacyConsentScreen.kt:120 | `fontSize = 12.sp` | `bodySmall` |
| PrivacyPolicyScreen.kt:157 | `lineHeight = 22.sp` | typography style |
| UserAgreementScreen.kt:135 | `lineHeight = 22.sp` | typography style |

**修复方案**：替换为 `MaterialTheme.typography.*` 对应样式。

---

### 低严重度（6 项）

#### L1. HomeScreen 内部 Spacer 高度不一致

**文件**：`HomeScreen.kt:124,236,285,313`

同一文件内 SectionTitle 后的 Spacer 高度在 8dp 和 12dp 之间切换。

**修复方案**：统一为 8.dp。

---

#### L2. FilterChip selectedLabelColor 统一用 Color.White

**文件**：`HomeScreen.kt:763,781,897,907,924`、`RemindersScreen.kt:531,549,598,608,625`

共 10 处 `selectedLabelColor = Color.White`。应使用 `MaterialTheme.colorScheme.onPrimary`。

---

#### L3. HomeScreen 功能网格额外嵌套 padding

**文件**：`HomeScreen.kt:319`

LazyColumn 已有 `padding(horizontal = 20.dp)`，功能网格又额外 `padding(horizontal = 16.dp)`，导致功能区域实际边距为 36dp。

**修复方案**：移除功能网格的内层 `padding(horizontal = 16.dp)`（同时修复 M1 后外层变为 16dp）。

---

#### L4. MedicalRecordScreen 卡片头部/底部背景 ad-hoc 半透明

**文件**：`MedicalRecordScreen.kt:528,664`

```kotlin
.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
.background(MaterialTheme.colorScheme.surface)
```

虽然使用了 colorScheme，但 `primary.copy(alpha=0.05f)` 是 ad-hoc 处理，且 surface 作为底部背景在深色模式下与 cardContainerColor 可能不一致。

---

#### L5. TrendChart Canvas 文字 fontSize 硬编码

**文件**：`TrendChart.kt:272,274`

```kotlin
TextStyle(fontSize = 9.sp, color = textColor)
```

图表标签 9sp 硬编码。Canvas 绘制的特殊场景，可接受，但如果需要可提取为常量。

---

#### L6. ProfileScreen 中 `!!` 强解包

**文件**：`ProfileScreen.kt:99,100`

```kotlin
val (bg, content) = memberCardColors(defaultMember!!.relation, defaultMember!!.gender)
```

在 `if (defaultMember != null)` 块内使用 `!!`。应使用 smart cast 或 `let`。

---

## 三、汇总统计

| 严重程度 | 数量 |
|---------|------|
| 高 | 8 |
| 中 | 11 |
| 低 | 6 |
| **合计** | **25** |

---

## 四、修复优先级建议

### 第一批（深色模式致命问题）

1. **H1** — GradientTopBar 深色模式修复（影响所有页面）
2. **H2** — MedicalRecordScreen 不可见图标修复
3. **H3** — SettingsScreen 保存按钮修复
4. **H4** — SettingsScreen Switch 修复

### 第二批（间距/样式统一）

5. **M1** — HomeScreen 水平边距统一为 16dp
6. **M2** — Card 内 padding 统一为 16dp
7. **M3** — LazyColumn 间距统一为 16dp
8. **M4** — Card elevation 统一为 SoftElevation
9. **M5** — Card shape 统一为 AppShapes.large
10. **M6** — PremiumScreen Card 样式补全

### 第三批（触摸目标 + 主题迁移）

11. **H7** — 编辑/删除按钮高度修复
12. **H8** — FamilyScreen 删除按钮修复
13. **H5** — ProfileScreen Premium 卡片 onPrimary
14. **H6** — OnboardingScreen onPrimary
15. **M7/M8** — 顶层变量迁移到 MaterialTheme.colorScheme
16. **M11** — 硬编码 fontSize/fontWeight 迁移到 typography

### 第四批（低优先）

17. **M9** — caption 扩展属性深色模式适配
18. **M10** — MemberColors/HealthTagColors 深色模式适配
19. **L1-L6** — Spacer 统一、FilterChip 语义色、嵌套 padding 等

---

## 五、核心结论

1. **深色模式几乎不可用**：GradientTopBar、Switch、保存按钮、SectionTitle 都硬编码了浅色值，是最高优先级问题。
2. **间距/卡片样式无统一规范**：Card padding 有 4 种值、LazyColumn 间距有 3 种值、Card shape/elevation 也有混用。
3. **直接引用顶层颜色变量**：大量 Screen 绕过 `MaterialTheme.colorScheme` 直接使用 `Primary`、`TextPrimary` 等，导致深色模式不自动切换。
4. **触摸目标仍有遗漏**：3 个按钮高度 32dp，FamilyScreen 删除按钮过小。
