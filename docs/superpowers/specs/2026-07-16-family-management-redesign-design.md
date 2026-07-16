# 智药乐 App 重构设计：导航调整 + 家庭管理独立 + 成员结构化信息 + 主页重设计

日期：2026-07-16
状态：已确认，待实现

## 目标

1. 调整全局导航：底部导航改为 3 个 tab（首页 / 家庭 / 医疗记录）；设置移到右上角齿轮，并合并「关于」区块。
2. 将「家庭管理」从设置页剥离，成为独立底部 tab（家庭）。
3. 家庭成员新增结构化个人信息与医疗注意事项字段，并内置常用预设快捷选择。
4. 主页重新设计为「成员详情式」：点击成员后下方展示该成员个人信息、医疗注意事项、最新医疗记录与健康贴士。

## 一、导航结构

### 底部导航（3 tab，顺序固定）
- 首页 `home`（图标 Home）
- 家庭 `family`（图标 People / Groups）
- 医疗记录 `medical_records`（图标 Person / Receipt）

移除原「设置」「关于」底部 tab。

### 右上角设置入口
- 在 首页 / 家庭 / 医疗记录 三个页面的 `TopAppBar` 右上角加齿轮图标（`Icons.Default.Settings`），点击 `navController.navigate("settings")`。
- 设置页内部包含「关于」区块（应用名、版本号等静态信息），不再单独成页。

### 路由变更（MainActivity.kt NavHost）
- 删除 `Screen.Settings`、`Screen.About` 两个 sealed 对象与对应 `composable`。
- 新增 `Screen.Family`（`route="family"`，图标 `Icons.Filled.People`，标签「家庭」）。
- 新增 `composable("family") { FamilyScreen(database, navController) }`。
- 保留 `composable("settings") { SettingsScreen(database) }`（从底部 tab 列表移除，但路由与 composable 保留，供右上角跳转）。
- 删除 `composable(Screen.About.route)` 及其 `AboutScreen` 调用；`AboutScreen` 内容并入 `SettingsScreen`。

## 二、家庭成员数据模型（结构化字段）

### FamilyMember 实体新增字段
现有：`id, name, relation, isDefault`
新增：
- `gender: String = ""` —— 性别
- `birthday: String = ""` —— 生日（存为字符串，如 "1990-05-20"，不引入额外日期类型转换）
- `bloodType: String = ""` —— 血型（如 "A" / "O" / "AB" / "B"）
- `allergy: String = ""` —— 过敏史（逗号分隔的多值文本）
- `chronic: String = ""` —— 慢性病
- `medicationNote: String = ""` —— 用药注意
- `otherNote: String = ""` —— 其他备注

> 不引入独立「标签」实体；医疗注意事项字段以逗号分隔的纯文本存储，配合 UI 预设 chip 辅助填写。

### 数据库迁移
- `AppDatabase` schema 版本 **v6 → v7**。
- 沿用现有 `fallbackToDestructiveMigration()`（开发期升级即清库，符合当前项目约定）。
- `FamilyMemberDao` 无需新增查询，现有 `getAllMembers / insert / update / deleteById / getDefaultMember` 足够。

## 三、家庭页（FamilyScreen，新页面）

### 布局
- `Scaffold` + `TopAppBar(title="家庭", actions=设置齿轮)`。
- 可滚动 `Column`，一个 `SettingsSection`-风格容器列出所有成员。
- 每个成员行：左侧头像图标 + 姓名（+ 关系/默认标识），右侧「编辑」「删除」按钮（沿用现有 SettingsScreen 逻辑）。
- 底部「新增家庭成员」按钮（全宽 OutlinedButton + Add 图标）。

### 新增/编辑对话框（AddEditMemberDialog）
字段与输入方式：
- 姓名（必填，OutlinedTextField）
- 关系（OutlinedTextField，可选，如 本人/父亲/子女）
- 性别（可选：单选 SegmentedButton / Row of FilterChip：男/女/其他，或 OutlinedTextField）
- 生日（OutlinedTextField，提示格式 yyyy-MM-dd）
- 血型（OutlinedTextField 或预设 chip：A/B/AB/O）
- 过敏史（OutlinedTextField + **预设 chip**：青霉素、头孢、海鲜、花粉、鸡蛋、牛奶）
- 慢性病（OutlinedTextField + **预设 chip**：高血压、糖尿病、心脏病、哮喘、痛风、慢性胃炎）
- 用药注意（OutlinedTextField + **预设 chip**：餐前服、餐后服、忌酒、定期复查）
- 其他备注（OutlinedTextField，多行）

预设 chip 行为：点击预设 → 追加到对应字段（逗号分隔，去重）；字段仍可自由手输。
保存：新增走 `insert(FamilyMember(...))`；编辑走 `editingMember.copy(...)`。

## 四、设置页（SettingsScreen）调整

- 移除「家庭成员管理」`SettingsSection`。
- 保留「AI 识别设置」「外观设置」。
- 新增「关于」`SettingsSection`：应用名「智药乐」、版本号（读 `BuildConfig.VERSION_NAME`，或硬编码字符串）、一句话简介。
- `TopAppBar` 无需再放设置齿轮（本就是设置页）；但为统一，右上角齿轮仅在 首页/家庭/医疗记录 显示。

## 五、主页重设计（成员详情式）

### 布局（HomeScreen）
- `Scaffold` + `TopAppBar(title="智药乐", actions=设置齿轮)`。
- 单个 `LazyColumn`（padding 16.dp，spacedBy 16.dp）：
  1. **成员选择行**：横向滚动 Row，每个成员一张可点击卡片（图标 + 姓名 + 关系），选中高亮（复用现有 `SelectedMemberHolder.homeSelectedMemberId`）。
  2. **个人信息卡**：标题「个人信息」，展示 姓名、关系、性别、生日、血型（仅非空项；关系/默认可合并显示）。
  3. **医疗注意事项卡**：标题「医疗注意事项」，展示 过敏史、慢性病、用药注意、其他备注（仅非空行；某字段为空则不显示该行）。
  4. **最新医疗记录卡**：该成员最近一条记录（诊断、就诊时间、医院、药品），复用 `MedicalRecordCard(record)`；无记录时显示 `EmptyRecords()` 或「该成员暂无医疗记录」。
  5. **健康贴士卡**：`HealthTipsCard()`。

### 数据加载
- 成员列表：`getAllMembers()` Flow（沿用）。
- 选中成员：`SelectedMemberHolder.homeSelectedMemberId`（沿用，默认最新记录所属成员或首个成员）。
- 选中成员的个人信息/注意事项：直接来自 `members.firstOrNull { it.id == selectedMemberId }`，无需新查询。
- 最新记录：`getRecentRecordsByMember(id, 1)`（沿用现有 DAO）。

## 六、受影响文件清单

- `MainActivity.kt`：nav 结构调整（Screen 密封类、NavHost、右上角齿轮入口 helper）。
- `ui/screens/FamilyScreen.kt`（新增）。
- `ui/screens/SettingsScreen.kt`：移除家庭管理区块、加关于区块。
- `ui/screens/HomeScreen.kt`：重设计为成员详情式、加右上角齿轮。
- `ui/screens/MedicalRecordScreen.kt`：加右上角齿轮（导航一致）。
- `data/model/FamilyMember.kt`：加结构化字段。
- `data/AppDatabase.kt`：v6 → v7。
- `AboutScreen.kt`：内容并入设置后，composable 调用移除（文件可保留或删除，倾向删除调用、保留文件无害；本设计选择删除 `AboutScreen` 路由调用，文件保留以备复用）。
- `ui/state/SelectedMemberHolder.kt`：不变。

## 七、验证方式

- `./gradlew :app:assembleDebug` 编译通过。
- 安装到设备 `d8cbc46f`，启动无崩溃。
- 手动验证：底部 3 tab 切换；右上角齿轮进设置；家庭 tab 增删改成员（含预设 chip）；主页点成员切换，下方展示个人信息/注意事项/最新记录/贴士；设置页含关于区块。
- 升级 v7 后旧数据被清（开发期预期行为，符合 `fallbackToDestructiveMigration` 约定）。
