# 医迹 (MedTrace) 页面布局审查报告

> 审查日期：2026-07-30 | 版本：v3.8.1 (versionCode 30)

---

## 一、审查范围

- `ui/screens/` - 全部 16 个 Screen
- `ui/components/` - 全部 9 个公共组件
- `MainActivity.kt` - 导航结构

---

## 二、问题清单（共 27 项）

### 高优先级（7 项）

#### H1. MemberEditDialog 内容过多，应改为全屏页面或 BottomSheet

**文件**：`MemberEditDialog.kt:86-244`
**维度**：对话框与弹窗

**问题**：AlertDialog 包含 10+ 个表单项（头像、姓名、关系、性别、生日、血型、过敏史、慢性病、用药注意、备注），分组为 4 个 section。小屏设备上对话框内滚动体验差，按钮被挤到屏幕边缘。

**建议**：改为 `ModalBottomSheet`，底部按钮符合拇指热区，天然支持长表单滚动。

---

#### H2. AddMedicalRecordScreen 保存/取消按钮在滚动底部，不在拇指热区

**文件**：`AddMedicalRecordScreen.kt:520-592`
**维度**：交互布局

**问题**：保存和取消按钮位于 `verticalScroll` Column 最底部。用户填写多个用药项后需滚动很长距离才能到达保存按钮，违反拇指热区原则。

**建议**：将按钮移到 Scaffold 的 `bottomBar`，始终可见。

---

#### H3. MedicalRecordScreen 顶部三张卡片信息过载

**文件**：`MedicalRecordScreen.kt:173-439`
**维度**：信息层级与视觉重点

**问题**：LazyColumn 前三个 item（统计概览、成员选择、搜索筛选）占据约 2-3 屏首屏空间。用户核心目标是查看就诊记录，但记录被推到很下方。

**建议**：
1. 成员选择器移到 TopBar 的 actions/subtitle 区域
2. 搜索筛选默认折叠，点击展开
3. 统计概览缩小为 TopBar subtitle（如"共 12 条记录"）

---

#### H4. MedicalRecordCard 存在两套不同实现

**文件**：
- `MedicalRecordScreen.kt:511-692`（private，带头部色带+操作按钮）
- `components/MedicalRecordCard.kt:40-129`（public，简洁版）

**维度**：列表与卡片布局

**问题**：两套卡片视觉风格、信息排列、间距都不同，用户在不同页面看到同类数据呈现不一致。

**建议**：统一为一个组件，通过 `showActions` 参数控制是否显示操作按钮。删除 private 版本。

---

#### H5. AddMedicalRecordScreen AI 识别区在基本信息之前

**文件**：`AddMedicalRecordScreen.kt:238-386`
**维度**：信息层级与视觉重点

**问题**：布局顺序为：成员选择 -> AI 识别区 -> 基本信息 -> 用药 -> 备注。AI 识别占据大量空间但为辅助功能。不使用 AI 的用户必须滚过一大段才能到达诊断、医院等必填字段。

**建议**：调整顺序为：成员选择 -> 基本信息 -> 用药记录 -> AI 识别（可折叠）-> 备注。

---

#### H6. SettingsScreen 单页过长，无分区导航

**文件**：`SettingsScreen.kt:362-846`
**维度**：信息层级与视觉重点

**问题**：外观、安全锁屏、数据备份同步、AI 配置全部放在一个 `verticalScroll` 中。高级版用户展开后整页可能超过 4-5 屏。

**建议**：使用可折叠分组（默认折叠高级功能），或使用 Tab 分区导航。

---

#### H7. HomeScreen 缺少 FAB，主要操作埋在功能网格中

**文件**：`HomeScreen.kt:308-362`
**维度**：交互布局

**问题**：首页核心操作"添加就诊记录"被放在 2×2 功能网格中。空状态文案写"点击右下角按钮添加"，但实际没有右下角按钮，文案与 UI 矛盾。

**建议**：添加 FAB 直接导航到添加记录页面，保留功能网格作为次要导航或精简。

---

### 中优先级（10 项）

#### M1. RemindersScreen ReminderItem 行内按钮过多

**文件**：`RemindersScreen.kt:367-451`

**问题**：每行包含 Checkbox + 内容 + 日期 + 标签 + 成员名 + 编辑按钮 + 删除按钮，小屏上内容区被严重挤压。

**建议**：编辑/删除改为长按菜单或滑动操作，或仅在展开时显示。

---

#### M2. AddTodoDialog 表单字段多，应考虑 BottomSheet

**文件**：`HomeScreen.kt:672-864`

**问题**：AlertDialog 包含 5 个表单字段（内容、类别、成员、日期、重复），AlertDialog 中体验受限。

**建议**：改为 `ModalBottomSheet`。

---

#### M3. MedicalRecordScreen 使用手动"加载更多"按钮，非无限滚动

**文件**：`MedicalRecordScreen.kt:421-438`

**问题**：分页使用手动 `OutlinedButton`，已用 `LazyColumn` 应利用滚动状态自动触发加载。

**建议**：监听 LazyColumn 滚动状态，接近底部时自动加载。

---

#### M4. HomeScreen TodayTodoItem 不同类别视觉高度不一致

**文件**：`HomeScreen.kt:408-575`

**问题**：根据 `todo.category` 显示完全不同的副内容，导致同一列表各 item 高度差异大，视觉不整齐。

**建议**：统一副内容区最小高度，使用一致的布局结构。

---

#### M5. MedicalRecordScreen 图标语义不当

**文件**：`MedicalRecordScreen.kt:585,643`

**问题**：医院和备注字段都使用 `Icons.Default.Settings`（齿轮），语义混淆。

**建议**：医院用 `LocalHospital` 或 `Business`，备注用 `Notes` 或 `Comment`。

---

#### M6. TrendsScreen MemberSelector 无卡片包裹

**文件**：`TrendsScreen.kt:77-88`

**问题**：MedicalRecordScreen 中 MemberSelector 有 Card 包裹，TrendsScreen 中裸放，风格不统一。

**建议**：统一使用 `SectionCard` 包裹。

---

#### M7. OnboardingScreen 无法返回上一页

**文件**：`OnboardingScreen.kt:91-96`

**问题**：引导页只有"下一步"/"跳过"，无法返回查看上一个引导页。

**建议**：添加 `BackHandler` 或返回按钮。

---

#### M8. ProfileScreen HelpDialog 内容可能溢出

**文件**：`ProfileScreen.kt:386-435`

**问题**：AlertDialog 中放了 5 个 HelpItem（约 10 段文本），容易超出屏幕高度。

**建议**：给 text 区域添加 `verticalScroll`，或改为全屏页面。

---

#### M9. PremiumScreen 全部使用硬编码中文字符串

**文件**：`PremiumScreen.kt` 全文

**问题**：所有 UI 文本硬编码，未使用 `stringResource()`，与项目规范不一致。

**建议**：提取到 `strings.xml`。

---

#### M10. MedicalRecordCard 操作按钮行背景色与卡片不一致

**文件**：`MedicalRecordScreen.kt:660-689`

**问题**：操作行用 `surface`，卡片主体用 `cardContainerColor()`，深色模式下可能有色差。

**建议**：操作行背景统一使用 `cardContainerColor()`。

---

### 低优先级（10 项）

#### L1. HealthTipsCard / FormTextField / 多个 EmptyState 组件是死代码

**文件**：`HealthTipsCard.kt`、`FormTextField.kt`、`EmptyStateComponents.kt:113-173`

**建议**：提及但不删除（按项目约定）。占用维护成本且可能误导开发者。

---

#### L2. FamilyScreen 使用 verticalScroll 而非 LazyColumn

**文件**：`FamilyScreen.kt:92-97`

**建议**：改为 `LazyColumn`，支持回收。成员卡片展开时内存占用更低。

---

#### L3. LockScreen 数字键盘按钮缺乏视觉反馈

**文件**：`LockScreen.kt:200-209`

**建议**：使用 `Surface` 加背景色和圆形 shape，与系统键盘视觉对齐。

---

#### L4. MemberDetailScreen Tab 标签硬编码中文

**文件**：`MemberDetailScreen.kt:41-46`

**建议**：改为 string resource。

---

#### L5. HomeScreen 底部 padding 硬编码 80dp

**文件**：`HomeScreen.kt:118`

**建议**：减小为 24-32dp，避免列表底部空白过多。

---

#### L6. MedicalRecordScreen 统计概览在分页数据上统计

**文件**：`MedicalRecordScreen.kt:219-224`

**问题**：在已分页的 records 列表（最多 20 条）上统计月度数据，数字误导。

**建议**：从数据库查询完整统计，或标注"当前列表中"。

---

#### L7. TrendsScreen 统计卡片三列数据无分隔线

**文件**：`TrendsScreen.kt:141-189`

**建议**：列间添加 `VerticalDivider`。

---

#### L8. FamilyScreen HealthDashboard 四个 StatCard 小屏拥挤

**文件**：`FamilyScreen.kt:480-512`

**建议**：使用 `FlowRow` 自动换行，或改为 2×2 网格。

---

#### L9. AddMedicalRecordScreen 用药删除按钮视觉过重

**文件**：`AddMedicalRecordScreen.kt:457-465`

**建议**：`OutlinedButton` 改为 `IconButton`，与项目其他删除操作一致。

---

#### L10. 底部导航栏缺少选中状态动画过渡

**文件**：`MainActivity.kt:340-372`

**建议**：使用 M3 的 `NavigationBar` + `NavigationBarItem`，或添加 `animateContentSize`。

---

## 三、汇总

| 优先级 | 数量 | 状态 | 核心问题 |
|--------|------|------|----------|
| 高 | 7 | ✅ 已修复 | 对话框过载、保存按钮位置、信息优先级错位、组件重复、缺 FAB、设置页过长 |
| 中 | 10 | ✅ 已修复 | 行内按钮拥挤、加载方式、图标语义、字符串硬编码、风格不统一 |
| 低 | 10 | ✅ 已修复 | 死代码、滚动容器、视觉细节、响应式适配 |

---

## 四、修复优先级建议

### 第一批（核心交互流程）
1. H2 - AddMedicalRecordScreen 保存按钮移至 bottomBar
2. H5 - AddMedicalRecordScreen AI 识别区移到基本信息之后
3. H7 - HomeScreen 添加 FAB
4. H3 - MedicalRecordScreen 顶部卡片精简

### 第二批（对话框/长页面优化）
5. H1 - MemberEditDialog 改为 BottomSheet
6. H6 - SettingsScreen 可折叠分组
7. M2 - AddTodoDialog 改为 BottomSheet
8. M8 - ProfileScreen HelpDialog 添加滚动

### 第三批（组件统一 + 交互优化）
9. H4 - MedicalRecordCard 统一为一套实现
10. M1 - RemindersScreen 行内按钮优化
11. M3 - MedicalRecordScreen 无限滚动
12. M5 - 图标语义修正

### 第四批（低优先）
13. M4-M10 - 视觉统一、字符串提取等
14. L1-L10 - 死代码、响应式、动画等

---

## 五、核心结论

1. **表单页面保存按钮位置是最大问题**：AddMedicalRecordScreen 的保存按钮在滚动底部，用户需滚动很长距离。应移到 Scaffold bottomBar。
2. **对话框承载过多表单**：MemberEditDialog（10+ 字段）和 AddTodoDialog（5 字段）应改为 BottomSheet。
3. **信息优先级错位**：MedicalRecordScreen 顶部三张卡片将核心内容推到很下方；AddMedicalRecordScreen 的 AI 识别区在必填字段之前。
4. **缺少 FAB**：HomeScreen 的核心操作"添加记录"埋在功能网格中，空状态文案与 UI 矛盾。
5. **组件重复**：MedicalRecordCard 有两套实现，视觉不一致。
