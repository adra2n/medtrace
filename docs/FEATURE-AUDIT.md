# 医迹 (MedTrace) 功能审查报告

> 审查日期：2026-07-30 | 版本：v3.8.1 (versionCode 30)

---

## 一、功能全貌

### 功能清单（13 个模块）

| 模块 | 完成度 | 说明 |
|------|--------|------|
| 启动与引导 | ✅ 完整 | Splash → 隐私同意 → 引导页 → 应用锁 → 主页 |
| 家庭成员管理 | ✅ 完整 | CRUD、展开详情、健康看板、头像、删除回退 |
| 病历记录管理 | ✅ 完整 | CRUD、搜索、筛选、分页、统计概览 |
| AI 智能分析 | ⚠️ 基本完整 | 文本/图片分析可用；免费次数限制未接入 |
| 健康待办（基础） | ✅ 完整 | 创建、完成、删除、编辑、分类、统计 |
| 健康待办（疗程） | 🔶 半成品 | 数据模型已实现，UI 无设置入口 |
| 每日通知 | ⚠️ 基本完整 | 通知、防重可用；声音/震动设置未接入 |
| 健康趋势 | ✅ 完整 | 折线图、参考范围、异常标记、血压拆分 |
| 成员详情 | ✅ 完整 | 四个 Tab 页功能完整 |
| 应用锁 | ✅ 完整 | 生物识别、PIN、自动锁定、暴力保护 |
| 数据备份 | ✅ 完整 | JSON 导入导出、AES 加密、Gist 同步、CSV |
| VIP 系统 | ⚠️ 基本完整 | 注册码激活可用；免费限制未实际执行 |
| 桌面小组件 | ✅ 完整 | 今日待服药物数量 + 前 3 条待办 |

### 数据模型关系

```
FamilyMember (家庭成员)
  ├── 1:N → MedicalRecord (病历记录)
  │         ├── MedicationItem (用药项，内嵌 JSON)
  │         └── metricsJson (AI 提取的检查指标)
  │              └── → TrendsScreen 健康趋势图
  └── 1:N → HealthTodo (健康待办)
              └── → 每日通知 → 桌面小组件

UserSettings (全局设置)
  └── selectedMemberId → FamilyMember (当前选中成员)
```

### 核心数据流

```
家庭成员 → 添加病历 → [AI 分析] → 检查指标 → 健康趋势图
         → 创建待办 → 每日通知 → 桌面小组件
         → 数据备份 → JSON/Gist 加密同步
```

---

## 二、发现的问题与建议

### A. 严重问题（功能层面缺失）

#### A1. VIP 免费限制未实际执行

**位置**：`PremiumManager.canAddMember()`、`canUseAiToday()`、`canUseFeature()`

**问题**：三个限制方法已实现但从未被任何 UI 调用。免费用户可无限添加成员、无限使用 AI 分析，VIP 付费体系形同虚设。

**建议**：
1. `FamilyScreen` 添加成员前检查 `canAddMember()`，超限弹出升级引导
2. `AddMedicalRecordScreen` AI 分析前检查 `canUseAiToday()`，超限弹出升级引导
3. 设置页备份/AI 配置区域检查 `canUseFeature()`

**优先级**：高（影响商业化）

---

#### A2. 综合健康分析功能未接入

**位置**：`ComprehensiveAnalysisUseCase`

**问题**：已完整实现成员健康档案综合分析（趋势解读 + 个性化建议），支持 PII 脱敏，但从未被任何 ViewModel 或 Screen 调用。

**建议**：
1. 在成员详情页或健康趋势页添加"AI 健康报告"按钮
2. 调用 `ComprehensiveAnalysisUseCase.analyze()` 获取报告
3. 展示分析结果（趋势解读 + 建议）

**优先级**：高（已有代码只需连接）

---

#### A3. 疗程进度功能不完整

**位置**：`HealthTodo.durationDays`、`startDate`、`getProgress()`

**问题**：数据模型和进度计算已实现，首页也展示了进度条，但创建/编辑待办的对话框没有设置 `durationDays` 的 UI 入口，进度条永远不会显示。

**建议**：
1. `AddTodoDialog` / `EditTodoDialog` 添加"疗程天数"输入框
2. 创建时自动设置 `startDate = today`
3. 类别为"服药"时默认显示疗程输入

**优先级**：中（功能补全）

---

#### A4. 通知声音/震动设置未接入

**位置**：`UserSettings.enableNotificationSound`、`enableVibration`

**问题**：数据模型有字段，但设置页无 UI 开关，`ReminderHelper.showNotification()` 也未读取。

**建议**：
1. 设置页"安全与锁屏"或新建"通知设置"分组添加两个 Switch
2. `showNotification()` 中根据设置选择 NotificationChannel

**优先级**：低（设计文档承诺但未实现）

---

### B. 架构问题（代码层面）

#### B1. NavGraph.kt 是死代码

**问题**：定义了完整导航图但从未被引用，实际导航全在 `MainActivity.kt` 内联。

**建议**：删除 `NavGraph.kt`，或迁移 `MainActivity` 导航逻辑到 NavGraph。

---

#### B2. SettingsViewModel 是空壳

**问题**：`loadSettings()` 返回 mock 数据，`toggleDarkMode()` 不持久化。实际设置读写全在 `SettingsScreen` 内直接操作数据库（超 1000 行）。

**建议**：将 SettingsScreen 的数据操作下沉到 SettingsViewModel。

---

#### B3. RemindersViewModel 绕过 Repository

**问题**：直接通过 `database.healthTodoDao()` 操作，而 `TodoRepository` 已存在但未使用。

**建议**：RemindersViewModel 改为注入 `TodoRepository`。

---

#### B4. MedicalRecordViewModel 搜索参数被忽略

**问题**：`loadRecords()` 接受 keyword/fromDate/toDate 但实际忽略，只用 memberId 查询。搜索逻辑在 Screen 内直接调用 DAO。

**建议**：将搜索筛选逻辑下沉到 ViewModel。

---

### C. 功能增强建议

#### C1. 提醒页缺少按成员筛选

**问题**：DESIGN.md 提到"按成员筛选待办"，但 RemindersScreen 没有成员筛选 UI。

**建议**：添加 MemberSelector 或 FilterChip。

---

#### C2. 病历列表支持按就诊时间排序

**当前**：仅按时间倒序。

**建议**：添加排序选项（按时间/按医院/按诊断）。

---

#### C3. 健康趋势支持自定义时间范围

**当前**：固定展示近 365 天。

**建议**：添加时间范围选择器（30 天/90 天/365 天/全部）。

---

#### C4. 数据导出支持 Excel 格式

**当前**：支持 JSON 和 CSV。

**建议**：增加 Excel 导出，方便打印给医生看。

---

#### C5. 病历记录支持附件图片

**当前**：AI 分析时上传图片但不保存原图。

**建议**：保存原始处方/报告图片到病历记录，方便日后查看。

---

#### C6. 用药提醒支持具体时间点

**当前**：每天 9:00 统一通知。

**建议**：支持为每个待办设置具体提醒时间（如 8:00 早、12:00 午、20:00 晚）。

---

### D. 商业化建议

#### D1. 支付系统未完成

**当前**：手动注册码模式。

**建议**：接入华为 IAP 或支付宝/微信支付。

---

#### D2. 友盟开屏广告未接入

**当前**：只有兜底启动页，无实际广告。

**建议**：接入友盟 U-AppWin 开屏广告 SDK，或移除相关代码。

---

## 三、优先级排序

| 序号 | 问题 | 优先级 | 工作量 | 类型 |
|------|------|--------|--------|------|
| A1 | VIP 免费限制未执行 | 🔴 高 | 中 | 功能缺失 |
| A2 | 综合健康分析未接入 | 🔴 高 | 小 | 功能缺失 |
| A3 | 疗程进度 UI 入口 | 🟡 中 | 小 | 功能补全 |
| B1 | NavGraph 死代码 | 🟡 中 | 小 | 架构清理 |
| B3 | RemindersViewModel 绕过 Repository | 🟡 中 | 小 | 架构修复 |
| B4 | MedicalRecordViewModel 参数忽略 | 🟡 中 | 中 | 架构修复 |
| B2 | SettingsViewModel 空壳 | 🟡 中 | 大 | 架构修复 |
| C6 | 用药提醒支持具体时间点 | 🟡 中 | 中 | 功能增强 |
| C1 | 提醒页按成员筛选 | 🟢 低 | 小 | 功能增强 |
| C3 | 趋势页自定义时间范围 | 🟢 低 | 小 | 功能增强 |
| A4 | 通知声音/震动设置 | 🟢 低 | 小 | 功能补全 |
| C5 | 病历附件图片 | 🟢 低 | 中 | 功能增强 |
| C2 | 病历排序选项 | 🟢 低 | 小 | 功能增强 |
| C4 | Excel 导出 | 🟢 低 | 中 | 功能增强 |
| D1 | 支付系统 | 🟢 低 | 大 | 商业化 |
| D2 | 开屏广告 | 🟢 低 | 中 | 商业化 |

---

## 四、核心结论

1. **核心功能完成度约 85%**，数据安全和使用体验做得扎实
2. **最大短板是 VIP 体系未执行**（A1），免费用户无任何限制，付费形同虚设
3. **已有但未接入的功能**（A2 综合分析、A3 疗程进度）只需少量工作即可激活
4. **架构层面有 4 个不一致**（B1-B4），影响可维护性但不影响用户
5. **功能增强方向**主要集中在提醒自定义（C6）、数据筛选（C1/C3）、附件管理（C5）
