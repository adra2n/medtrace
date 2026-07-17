# UI 保守打磨设计方案（2026-07-17）

## 目标
在**不改变任何功能逻辑**的前提下，对医迹（chiyaole）整体 UI 做展示层一致性打磨：统一间距、圆角、字号层级、卡片外观与顶部栏，使各页面风格更精致、克制、一致。

方向：**保守打磨**——保留现有青绿/沙金配色（`Primary`/`Accent`）、`AppShapes`、`cardContainerColor()` 设计令牌与全部组件结构，不引入渐变、不放大视觉冲击。

## 改动清单

### 1. 统一间距与圆角（设计令牌）
- 各屏幕统一外边距 `16.dp`、卡片内边距 `16.dp`、卡片间距 `16.dp`。
- 消除当前不一致：Home 用 `spacedBy(16)`、Family 用 `spacedBy(12)`、Records 列表用 `16`、`padding(horizontal=16)` 等。
- `AppShapes` 保持现状（extraSmall 8 / small 10 / medium 14 / large 20 / extraLarge 28）。

### 2. 卡片视觉一致性
统一以下卡片使用：`cardContainerColor()` 底 + `AppShapes.medium` + `1.dp` elevation + `16.dp` 内边距：
- `SectionCard`
- `MedicalRecordCard`
- `MemberProfileCard`（FamilyScreen）
- `SettingsSection`
标题文字层级统一：区块标题 `titleMedium` + `primary`；字段标签 `labelMedium` + `onSurfaceVariant`（修正 SectionCard 当前用 `titleSmall`、MedicalRecordCard 用 `labelMedium` 的不一致）。

### 3. 顶部栏（TopAppBar）统一
- Home / Trends / Records / Family / Settings 的 `TopAppBar` 统一标题字号与颜色层级。
- Home 双行标题（医迹 + 家庭健康管理）保留，微调次级文字层级使其更清爽。

### 4. 空状态与按钮
- `EmptyRecords` / `EmptyFamily` 统一图标容器尺寸（`72.dp`）与间距节奏。
- 主要操作按钮统一为全宽圆角 `Button`（去添加记录、保存等）；设置页底部栏 取消/保存 保持。

### 5. 首页健康趋势卡片
- 趋势图卡片内 `AI 综合分析` 按钮与 `AI 趋势解读` 结果卡的间距/层级微调，与全站风格统一；不改动触发逻辑。

## 不改动
- 任何业务逻辑、数据流向、导航路由、AI 调用、权限请求、对话框逻辑。
- 配色值、`AppShapes`、`cardContainerColor()` 的实现语义。

## 实施原则
以修改 `ui/theme` 与共用组件（`SectionCard`、`MedicalRecordCard`、`MemberSelector`、`TrendChart`）为主，屏幕层仅做间距/层级收口，避免大改屏幕代码。优先动共享组件，减少各屏幕重复调整。
