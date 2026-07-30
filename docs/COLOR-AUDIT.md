# 医迹 (MedTrace) 配色体系审计报告

> 审计日期：2026-07-30 | 版本：v3.8.1 (versionCode 30)

---

## 一、审计范围

- `Color.kt` - 32 个顶层颜色变量 + 2 个 data class + 2 个颜色映射函数 + 1 个扩展属性
- `Theme.kt` - LightColorScheme / DarkColorScheme 映射
- `Design.kt` - 主题组件（GradientTopBar、SectionTitle 等）
- 全部 14 个 Screen + 全部公共组件
- 对比度标准：WCAG 2.1 AA（正常文字 ≥ 4.5:1，大字 ≥ 3:1）

---

## 二、对比度问题（P0 - 影响所有用户）

### C1. Primary 按钮背景 + 白色文字对比度严重不足（1.97:1）

`Primary(#2ECDC6)` 是主按钮背景色，`onPrimary` 在 Theme.kt 中固定为白色 `#FFFFFF`。

| 前景 | 背景 | 对比度 | WCAG AA (4.5:1) | WCAG AA Large (3:1) |
|------|------|--------|-----------------|---------------------|
| #2ECDC6 | #FFFFFF | **1.97:1** | ❌ FAIL | ❌ FAIL |

**影响范围**：
- 直接使用 `Primary` 作为按钮背景：`PremiumScreen.kt:206`、`PrivacyConsentScreen.kt:100`
- 通过 `MaterialTheme.colorScheme.primary` 间接使用：**115 处**（分布在 15+ 个文件）
- 所有 Material3 默认 Button 的 `containerColor = primary` + `contentColor = onPrimary(白色)` 按钮文字不可读

**修复建议**：将 Primary 加深至 `#008B85`（对比度 ~4.6:1，满足 WCAG AA），同步调整 `PrimaryGradientEnd`、`onPrimary` 等。

---

### C2. TextDisabled 在 Background 上对比度不足（2.37:1）

`TextDisabled(#98A2B3)` 通过 `caption` 扩展属性使用，背景为 `Background(#F0F7F7)`。

| 前景 | 背景 | 对比度 | WCAG AA | WCAG AA Large |
|------|------|--------|---------|---------------|
| #98A2B3 | #F0F7F7 | **2.37:1** | ❌ FAIL | ❌ FAIL |

**修复建议**：加深至 `#6B7280`（对比度 ~4.5:1）。

---

### C3. Urgent 白底对比度不足（2.84:1）

`Urgent(#FF6B35)` 在 HomeScreen 中用作标签文字色，背景为白色卡片。

| 前景 | 背景 | 对比度 | WCAG AA | WCAG AA Large |
|------|------|--------|---------|---------------|
| #FF6B35 | #FFFFFF | **2.84:1** | ❌ FAIL | ❌ FAIL |

**修复建议**：加深至 `#D84315`（对比度 ~4.5:1）。

---

### C4. Healthy 白底对比度不足（2.78:1）

`Healthy(#4CAF50)` 在 HomeScreen、FamilyScreen 中用作标签文字色。

| 前景 | 背景 | 对比度 | WCAG AA | WCAG AA Large |
|------|------|--------|---------|---------------|
| #4CAF50 | #FFFFFF | **2.78:1** | ❌ FAIL | ❌ FAIL |

**修复建议**：加深至 `#2E7D32`（对比度 ~5.1:1），或仅在深色背景上使用。

---

### 完整对比度表

| 前景 | 背景 | 对比度 | WCAG AA | WCAG AA Large | WCAG AAA | 状态 |
|------|------|--------|---------|---------------|----------|------|
| Primary #2ECDC6 | White #FFFFFF | 1.97:1 | ❌ | ❌ | ❌ | **按钮文字不可读** |
| TextDisabled #98A2B3 | Background #F0F7F7 | 2.37:1 | ❌ | ❌ | ❌ | **说明文字不可读** |
| Urgent #FF6B35 | White #FFFFFF | 2.84:1 | ❌ | ❌ | ❌ | 紧急标签不可读 |
| Healthy #4CAF50 | White #FFFFFF | 2.78:1 | ❌ | ❌ | ❌ | 健康标签不可读 |
| Primary #2ECDC6 | Background #F0F7F7 | 1.81:1 | ❌ | ❌ | ❌ | 主色在背景上不可读 |
| Success #2E9E5B | White #FFFFFF | 3.41:1 | ❌ | ✅ | ❌ | 大字可用 |
| Error #D64550 | White #FFFFFF | 4.35:1 | ❌ | ✅ | ❌ | 接近但不达标 |
| TextSecondary #5F6B7A | Surface #FFFFFF | 5.43:1 | ✅ | ✅ | ❌ | 正常 |
| TextPrimary #2E3A46 | Background #F0F7F7 | 10.69:1 | ✅ | ✅ | ✅ | 优秀 |

---

## 三、颜色冗余/重复定义（P1）

### H1. 六个变量共享同一值 `#E8F8F8`

| 变量名 | 值 | 是否被使用 | 说明 |
|--------|-----|-----------|------|
| `PrimaryLight` | 0xFFE8F8F8 | ❌ 仅 Design.kt 死导入 | 应删除 |
| `BgFamily` | 0xFFE8F8F8 | ❌ 完全未使用 | 死代码，应删除 |
| `BgTodo` | 0xFFE8F8F8 | ❌ 完全未使用 | 死代码，应删除 |
| `BgQuick` | 0xFFE8F8F8 | ❌ 完全未使用 | 死代码，应删除 |
| `CardSecondary` | 0xFFE8F8F8 | 仅 Theme.kt (`surfaceVariant`) | 保留 |
| Color.kt 内硬编码 | 0xFFE8F8F8 | `memberCardColorSets()` / `healthTagColorSets()` | 应引用变量 |

**修复建议**：删除 `PrimaryLight`、`BgFamily`、`BgTodo`、`BgQuick`，保留 `CardSecondary` 作为唯一命名。Color.kt 内硬编码改为引用 `CardSecondary`。

---

### H2. Warning 和 Accent 完全相同（`#FF7D60`）

| 变量名 | 值 | 是否被使用 |
|--------|-----|-----------|
| `Warning` | 0xFFFF7D60 | ❌ 完全未使用（0 处引用） |
| `Accent` | 0xFFFF7D60 | 仅 Theme.kt (`secondary`) |

两个语义不同的颜色（"警告" vs "强调"）使用相同色值。`Warning` 是死代码。

**修复建议**：删除 `Warning`。`Accent` 仅在 Theme.kt 中映射为 `secondary`，但 `colorScheme.secondary` 在 Screen/Component 中**零引用**，可考虑一并清理。

---

### H3. PrimaryDark 和 PrimaryGradientEnd 完全相同（`#25B5B0`）

| 变量名 | 值 | 是否被使用 |
|--------|-----|-----------|
| `PrimaryDark` | 0xFF25B5B0 | ❌ 完全未使用（0 处引用） |
| `PrimaryGradientEnd` | 0xFF25B5B0 | 仅 Design.kt (`PrimaryGradient`) |

`PrimaryDark` 是死代码。

**修复建议**：删除 `PrimaryDark`，保留 `PrimaryGradientEnd`。

---

### H4. Surface 和 CardSurface 完全相同（`#FFFFFF`）

| 变量名 | 值 | 是否被使用 |
|--------|-----|-----------|
| `Surface` | 0xFFFFFFFF | 仅 Theme.kt (`surface` in LightColorScheme) |
| `CardSurface` | 0xFFFFFFFF | ❌ 完全未使用 |

`CardSurface` 是死代码。

**修复建议**：删除 `CardSurface`。

---

### H5. CardSurfaceDark 和 SurfaceDark 完全相同（`#161B22`）

| 变量名 | 值 | 是否被使用 |
|--------|-----|-----------|
| `SurfaceDark` | 0xFF161B22 | 仅 Theme.kt (`surface` in DarkColorScheme) |
| `CardSurfaceDark` | 0xFF161B22 | 仅 Theme.kt (`cardContainerColor()`) |

两者各使用一次，可合并。

**修复建议**：删除 `CardSurfaceDark`，`cardContainerColor()` 改用 `SurfaceDark`。

---

## 四、功能色语义冲突（P2）

### M1. Success(#2E9E5B) vs Healthy(#4CAF50) - 绿色语义重叠

| 颜色 | 值 | 用途 | 使用文件 |
|------|-----|------|---------|
| `Success` | #2E9E5B | 成功状态 | TrendChart.kt（图表区域填充） |
| `Healthy` | #4CAF50 | 健康/正常状态 | HomeScreen.kt, FamilyScreen.kt |

两者都是绿色，在医疗记录 app 中"成功"与"健康"语义高度重叠。色值差异肉眼可辨但不大。

**修复建议**：合并为一个绿色 `#2E7D32`（同时解决 C4 对比度问题），或文档化区分使用场景。

---

### M2. Error / Urgent / Warning - 红/橙红色域拥挤

| 颜色 | 值 | 白底对比度 | 用途 | 使用情况 |
|------|-----|-----------|------|---------|
| `Error` | #D64550 | 4.35:1 | 错误状态 | 19 处（colorScheme.error） |
| `Urgent` | #FF6B35 | 2.84:1 | 紧急/重要 | 2 处（HomeScreen） |
| `Warning` | #FF7D60 | 2.51:1 | 警告 | 0 处（死代码） |

三个功能色挤在红橙色域，视觉容易混淆。`Warning` 已是死代码。

**修复建议**：删除 `Warning`。将 `Urgent` 加深至 `#D84315`（解决 C3 对比度问题）与 `Error` 拉开距离。

---

### M3. Warning(#FF7D60) vs Reminder(#FF9800) - 橙色系冗余

| 颜色 | 值 | 语义 | 使用情况 |
|------|-----|------|---------|
| `Warning` | #FF7D60 | 警告 | 0 处（死代码） |
| `Reminder` | #FF9800 | 提醒/待办 | 3 处 |

`Warning` 完全未使用，与 `Reminder` 同属橙色系。

**修复建议**：删除 `Warning`。

---

## 五、配色层次冗余（P3）

### L1. 8 个完全死代码颜色变量

| 变量名 | 值 | 说明 |
|--------|-----|------|
| `PrimaryDark` | #25B5B0 | 与 PrimaryGradientEnd 重复 |
| `PrimarySoft` | #7DD8D3 | 从未使用 |
| `AccentDark` | #E85C3C | 从未使用 |
| `Warning` | #FF7D60 | 与 Accent 重复，从未使用 |
| `BgFamily` | #E8F8F8 | 与 CardSecondary 重复 |
| `BgTodo` | #E8F8F8 | 与 CardSecondary 重复 |
| `BgQuick` | #E8F8F8 | 与 CardSecondary 重复 |
| `CardSurface` | #FFFFFF | 与 Surface 重复 |

**修复建议**：全部删除。

---

### L2. Design.kt 3 个死导入

`Design.kt:24-26` 导入了 `PrimaryLight`、`TextPrimary`、`TextSecondary`，但函数体中从未使用。

**修复建议**：删除这 3 个 import。

---

### L3. 主色变体过多（5 个中 3 个未使用）

| 变量名 | 值 | 直接引用次数（排除 Color.kt/Theme.kt） |
|--------|-----|---------------------------------------|
| `Primary` | #2ECDC6 | 8（3 Screen + Design.kt） |
| `PrimaryLight` | #E8F8F8 | 0（仅 Design.kt 死导入） |
| `PrimaryDark` | #25B5B0 | 0 |
| `PrimaryGradientEnd` | #25B5B0 | 1（Design.kt） |
| `PrimarySoft` | #7DD8D3 | 0 |

5 个主色变体中仅 2 个在实际使用。

**修复建议**：删除 `PrimaryLight`、`PrimaryDark`、`PrimarySoft`，保留 `Primary` 和 `PrimaryGradientEnd`。

---

### L4. Accent 系列仅 Theme.kt 使用，colorScheme.secondary 零引用

| 变量名 | 值 | 直接引用次数 | Theme.kt 映射 |
|--------|-----|-------------|--------------|
| `Accent` | #FF7D60 | 0 | `secondary` |
| `AccentLight` | #FFB29E | 0 | `secondary` (dark) |
| `AccentDark` | #E85C3C | 0 | 无 |

`Accent` 和 `AccentLight` 仅在 Theme.kt 中映射为 `secondary`，但 `colorScheme.secondary` 在 Screen/Component 中**零引用**。`AccentDark` 是完全死代码。

**修复建议**：删除 `AccentDark`。评估是否保留 `Accent`/`AccentLight` 的 secondary 映射。

---

## 六、颜色使用频率统计

### 直接引用（排除 Color.kt 和 Theme.kt）

| 颜色变量 | 值 | 引用次数 | 引用文件 |
|---------|-----|---------|---------|
| `Primary` | #2ECDC6 | 8 | MainActivity, PremiumScreen, PrivacyConsentScreen, Design.kt |
| `Healthy` | #4CAF50 | 3 | HomeScreen, FamilyScreen |
| `Reminder` | #FF9800 | 3 | HomeScreen, FamilyScreen, RemindersScreen |
| `Urgent` | #FF6B35 | 2 | HomeScreen |
| `NoStatus` | #9E9E9E | 2 | HomeScreen, FamilyScreen |
| `Info` | #2196F3 | 1 | HomeScreen |
| `Success` | #2E9E5B | 1 | TrendChart |
| `Background` | #F0F7F7 | 1 | MainActivity |
| `PrimaryGradientEnd` | #25B5B0 | 1 | Design.kt |
| 其余 23 个变量 | - | 0 | - |

### 通过 MaterialTheme.colorScheme 间接引用

| colorScheme 属性 | 映射的 Color.kt 变量 | 间接引用次数 |
|-----------------|---------------------|-------------|
| `primary` | Primary | 115 |
| `onSurfaceVariant` | TextSecondary | 85 |
| `onSurface` | TextPrimary | 32 |
| `onPrimary` | Color(0xFFFFFFFF) | 27 |
| `error` | Error | 19 |
| `outlineVariant` | Color(0xFFEFF4F8) | 12 |
| `outline` | Border | 8 |
| `surfaceVariant` | CardSecondary | 6 |
| `onPrimaryContainer` | Color(0xFF003542) | 6 |
| `primaryContainer` | Color(0xFFD6EEF6) | 5 |
| `surface` | Surface | 4 |
| `background` | Background | 3 |

---

## 七、修复优先级建议

### P0 - 对比度致命问题（影响可读性）

| # | 问题 | 修复方案 | 影响范围 |
|---|------|---------|---------|
| C1 | Primary #2ECDC6 白字对比度 1.97:1 | 加深至 `#008B85`（~4.6:1） | 115 处 |
| C2 | TextDisabled #98A2B3 对比度 2.37:1 | 加深至 `#6B7280`（~4.5:1） | caption 扩展 |
| C3 | Urgent #FF6B35 白底对比度 2.84:1 | 加深至 `#D84315`（~4.5:1） | 2 处 |
| C4 | Healthy #4CAF50 白底对比度 2.78:1 | 加深至 `#2E7D32`（~5.1:1） | 3 处 |

### P1 - 死代码清理

| # | 问题 | 修复方案 |
|---|------|---------|
| H1-H5 | 8 个完全死代码变量 | 删除 `PrimaryDark`、`PrimarySoft`、`AccentDark`、`Warning`、`BgFamily`、`BgTodo`、`BgQuick`、`CardSurface` |
| H1-H5 | 5 组重复色值 | 合并同名变量 |

### P2 - 语义/层次优化

| # | 问题 | 修复方案 |
|---|------|---------|
| M1 | Success/Healthy 语义重叠 | 合并为 `#2E7D32`，或文档化区分规则 |
| M2 | Error/Urgent/Warning 色域拥挤 | 删除 `Warning`，加深 `Urgent` 与 `Error` 拉开距离 |
| L2 | Design.kt 3 个死导入 | 删除 |
| L3 | 主色变体过多 | 删除 3 个未使用变体 |
| L4 | Accent 系列零引用 | 删除 `AccentDark`，评估 secondary 映射 |

### P3 - 代码质量

| # | 问题 | 修复方案 |
|---|------|---------|
| H1 | Color.kt 内硬编码 `Color(0xFFE8F8F8)` | 改为引用 `CardSecondary` 变量 |

---

## 八、核心结论

1. **Primary 色值太浅是最大问题**：`#2ECDC6` 作为主色在白色文字按钮上对比度仅 1.97:1，远低于 WCAG AA 标准的 4.5:1。建议加深至 `#008B85`。
2. **8 个死代码颜色变量**：`PrimaryDark`、`PrimarySoft`、`AccentDark`、`Warning`、`BgFamily`、`BgTodo`、`BgQuick`、`CardSurface` 定义了但从未使用。
3. **5 组颜色值完全重复**：同一色值被多个变量重复定义，增加维护成本。
4. **功能色对比度普遍不足**：`Urgent`、`Healthy` 在白底上均不达标，需要加深色值。
5. **32 个颜色变量中仅 9 个被直接使用**，大量冗余定义需要清理。
