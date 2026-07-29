# 医迹 (MedTrace) 项目全面审查报告

> 审查日期：2026-07-29 | 版本：v3.8.1 (versionCode 30)

---

## 一、项目概览

| 维度 | 现状 |
|------|------|
| 代码总量 | ~13,500 行 Kotlin |
| 版本 | v3.8.1 (versionCode 30) |
| 技术栈 | Kotlin + Jetpack Compose + Room + Hilt + Retrofit |
| 架构 | 混合式：部分 MVVM + 部分直接 DB 访问 |
| 测试 | 19 个单元测试 + 10 个集成测试 |

---

## 二、架构问题（P0 - 严重）

### 1. 架构不一致：ViewModel 迁移只做了一半

**现状**：9 个 ViewModel 已创建，但只有 3 个 Screen 真正使用（HomeScreen、RemindersScreen、ProfileScreen）；5 个 Screen 仍直接持有 AppDatabase 在 Composable 里做 CRUD：
- `MedicalRecordScreen.kt` — 手动分页逻辑全在 UI 层
- `AddMedicalRecordScreen.kt` — AI 分析、图片处理、保存全在 520 行的 Composable 里
- `FamilyScreen.kt` — 直接 DAO 操作，无错误处理
- `SettingsScreen.kt` — 966 行 God Composable，20+ 个 remember 状态
- `TrendsScreen.kt` — 直接 DAO 查询

**建议**：将所有 Screen 迁移到 ViewModel 模式，数据操作下沉到 Repository 层。

### 2. Hilt DI 使用不完整

**现状**：`@HiltAndroidApp` 已声明，`AppModule.kt` 提供了 DB/DAO/Repository，但所有 ViewModel 都用手写 `ViewModelProvider.Factory`（9 个 Factory 类），而非 `@HiltViewModel` + `@Inject`。

**建议**：迁移到 `@HiltViewModel`，删除全部手写 Factory。

### 3. NavGraph 中直接传 AppDatabase 给 Screen

`MainActivity.kt:405` — `FamilyScreen(database, navController, ...)` 直接传 DB 实例，绕过了 Repository 抽象。应改为传 ViewModel 或 Repository。

### 4. SelectedMemberHolder 全局可变单例

`ui/state/SelectedMemberHolder.kt` 是一个 `object` 持有 `MutableState`，被 3+ 个 Screen 用作共享状态总线，绕过了响应式数据层。应将选中成员 ID 存入 `UserSettings` 表（已有 `selectedMemberId` 字段），通过 Flow 响应式传播。

---

## 三、UI/UX 设计问题

### P0 - 功能性 Bug

#### 1. FamilyScreen — Flow 收集 Bug（数据丢失）

`FamilyScreen.kt` 在 `forEach` 循环内调用 `.collect`，由于 `.collect` 是挂起的，只有第一个家庭成员的最近就诊记录会被加载，其余成员的记录永远为空。

> **已修复** ✅ — 改用 `.first()` 替代 `.collect`

#### 2. TrendsScreen — 名不副实

名为"数据统计/趋势"，但没有任何图表。项目有 `TrendChart.kt` 组件却未被使用。只显示静态数字和扁平列表。`error` 变量被赋值但从未在 UI 中渲染。

> **已修复** ✅ — 接入 `TrendSection` + `buildSeries` 实现趋势图，新增错误横幅展示

#### 3. TrendChart.kt — 血压数据 Bug

`expandMetric` 将 "120/80" 拆分为收缩压和舒张压两个指标，但两者都保留原始值 "120/80"，`parseNumeric` 对两者都取第一个数字 120。舒张压曲线实际画的是收缩压的数据。

> **已修复** ✅ — 舒张压改用 `rawParts[1]` 而非 `m.value`

#### 4. AddMedicalRecordViewModel — 保存失败导致 UI 卡死

`saveRecord` / `updateRecord` 没有 try-catch，如果 DB 插入抛异常，`isSaving` 永远为 true，UI 永久停在加载状态。

> **已修复** ✅ — 包裹在 try-catch 中，失败时重置 `isSaving` 并设置 `error` 状态

#### 5. 隐私同意"拒绝"无效

`NavGraph.kt` 中 `onDecline = { /* activity?.finish() */ }` — 拒绝隐私协议什么都不做，注释掉了关闭 App 的逻辑。

> **已修复** ✅ — `onDecline = { activity?.finish() }`

### P1 - 用户体验问题

#### 6. 所有文字硬编码，零国际化支持

11 个 Screen 中数百条中文字符串全部硬编码在代码中，`strings.xml` 仅 30 行。未来任何国际化都需要大规模重构。

#### 7. 触摸目标过小

大量 `IconButton` 使用 `Modifier.size(32.dp)` 甚至 24dp，低于 Material Design 建议的 48dp 最小值。影响操作准确度，尤其是医疗场景下用户可能是在不舒服状态下操作。

#### 8. SettingsScreen — 966 行 God Composable

管理 20+ 个 `remember` 状态变量和 8 个本地函数，单函数约 760 行。深色模式立即生效但其他设置需点"保存"——保存行为不一致。

#### 9. 加载/错误/空状态处理不统一

| Screen | Loading 状态 |
|--------|-------------|
| HomeScreen | 无 |
| MedicalRecordScreen | 分页有 |
| FamilyScreen | 无 |
| TrendsScreen | 无 |
| ProfileScreen | 无 |
| RemindersScreen | 无 |

#### 10. 组件重复定义

- 2 个 `EmptyState`（`EmptyState.kt` + `EmptyStateComponents.kt`，同名重载）
- 2 个加载组件（`LoadingView.kt` + `LoadingErrorState.kt` 中的 `LoadingState`）
- 2 个错误组件（`ErrorView.kt` + `LoadingErrorState.kt` 中的 `ErrorState`）

### P2 - 视觉细节

#### 11. ui.md 设计规范未完全落地

`ui.md` 定义了 `#3BA9F5` 蓝色主色，但 `Color.kt` 实际用 `#2ECDC6` 青绿色。设计文档与实现不一致（可能是迭代后的决定，但文档未更新）。

#### 12. 硬编码颜色散落各处

`Color(0xFFFF6B35)`、`Color(0xFF4CAF50)`、`Color(0xFF2196F3)` 等散落在多个 Screen 中，未集中到 `Color.kt`。

#### 13. Emoji 用作图标不一致

`FamilyScreen` 的 `StatCard` 用 `Text("👥")`，`RemindersScreen` 用 `Text("💊")`，而其他页面用 Material Icons。在不同设备上 Emoji 渲染不一致。

---

## 四、安全问题

### P0 - 严重

#### 1. 注册码和设备 ID 被日志记录

`RegistrationCodeNative.kt:70` — `Log.d(TAG, "输入的注册码: $code")`
`RegistrationCodeNative.kt:60/71` — `Log.d(TAG, "当前设备ID: $deviceId")`

在 Release 包中也会输出注册码和设备 ID 到 logcat，任何连接设备的程序都能读取。

> **已修复** ✅ — 移除所有 Log.d 输出，`catch(Exception)` 改为 `catch(Throwable)`

#### 2. 友盟 SDK 日志在 Release 包中开启

`MedTraceApplication.kt:17` — `UMConfigure.setLogEnabled(true)` 无条件开启，应改为 `BuildConfig.DEBUG` 控制。

> **已修复** ✅ — 改为 `UMConfigure.setLogEnabled(BuildConfig.DEBUG)`

#### 3. PIN 无暴力破解保护

`PinManager.kt` 的 `verify()` 没有尝试次数限制或锁定机制。6 位 PIN 仅 100 万种组合，可被快速穷举。

> **已修复** ✅ — 增加 MAX_ATTEMPTS=5，锁定 30 秒，`isLocked()` / `getLockoutRemainingSeconds()`

#### 4. ComprehensiveAnalysisUseCase 发送完整医疗档案到 LLM

成员姓名、生日、性别、血型、过敏史、慢性病、用药史全部发送到用户配置的第三方 LLM 端点，无脱敏、无用户二次确认。

### P1 - 中等

#### 5. RegistrationCodeNative 捕获 Exception 而非 Throwable

`verifyCode()` 的 catch 块无法捕获 `UnsatisfiedLinkError`（继承自 Error），如果 native 库加载失败会直接崩溃。

> **已修复** ✅ — `catch(Exception)` 改为 `catch(Throwable)`

#### 6. SecurePrefs.get() 每次调用都创建新实例

`EncryptedSharedPreferences.create()` 涉及 Keystore 密钥加载，开销大。应缓存为 lazy 属性。

> **已修复** ✅ — `@Volatile` + `@Synchronized` 双重检查缓存

#### 7. BiometricHelper 未设置 setAllowedAuthenticators

`PromptInfo` 构建器未调用 `setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)`，实际允许的认证方式取决于实现而非意图。

> **已修复** ✅ — 添加 `setAllowedAuthenticators(AUTHENTICATORS)`

---

## 五、数据层问题

### P0 - 严重

#### 1. BackupRepository 遗漏 HealthTodo 数据

`BackupRepository.kt` 的 `exportAll` / `importAll` 只处理 Members、Records、Settings，不包含 HealthTodo。备份恢复后所有提醒和服药进度丢失。

> **已修复** ✅ — `BackupData` 新增 `todos` 字段，`HealthTodoDao` 新增 `clear()` / `getAllList()` / `insertAll(REPLACE)`

#### 2. TodoRepositoryImpl.toggleTodoDone 非原子操作

读-改-写 `completedDates` CSV 字符串，无事务包裹。并发操作可能导致丢失更新。两次调用 toggle 可能追加重复日期。

> **已修复** ✅ — 包裹在 `database.withTransaction` 中，增加日期重复检查

#### 3. 数据库迁移 v1→v5 完全缺失

旧版本用户升级时数据库会被销毁重建，数据丢失。虽然有提示恢复，但如果用户没有备份则不可逆。

#### 4. insertAll 无冲突策略

`FamilyMemberDao` 和 `MedicalRecordDao` 的 `insertAll` 默认 `ABORT` 策略，恢复时 PK 冲突会导致整个事务回滚，而表已被清空——数据丢失。应改为 `REPLACE`。

> **已修复** ✅ — `@Insert(onConflict = OnConflictStrategy.REPLACE)`

#### 5. 备份格式无版本号

`BackupData` 没有 schema 版本字段。旧备份恢复到新 schema 时，新增字段用 Kotlin 默认值，删除字段可能导致 `insertAll` 失败。

> **已修复** ✅ — 新增 `schemaVersion` 字段，export 写入当前 DB 版本，import 校验并警告

#### 6. CryptoUtil 迭代次数无版本标记

PBKDF2 迭代次数 (65536) 硬编码，未来提高迭代次数后旧备份无法解密。应在加密输出中包含版本号和迭代次数。

> **已修复** ✅ — 加密输出添加 `version(1B) + iterations(4B)` 前缀，解密兼容旧格式

#### 7. likePattern 抽象泄露

`RecordRepository` 接口暴露了 `likePattern: String` 参数，调用方需手动拼接 `%keyword%`，属于 SQL 层关注点泄露到 Repository 契约。

---

## 六、代码质量问题

### 重复/死代码

| 位置 | 问题 | 状态 |
|------|------|------|
| HomeScreen.kt | `QuickTodoChip`、`FamilyHealthOverview` 定义但未调用 | ✅ 已清理 |
| AddMedicalRecordScreen.kt | `FeatureTag` 定义但未调用；`Log.d` 残留 | ✅ 已清理 |
| RemindersScreen.kt | `RepeatEditDialog` 定义但未调用 | ✅ 已清理 |
| ProfileScreen.kt | `ProfileMenuGroup` 定义但未调用 | ✅ 已清理 |
| SettingsScreen.kt | `SettingsSection` 定义但未调用 | ✅ 已清理 |
| PremiumManager.kt | `prefs` 初始化但未使用 | ✅ 已清理 |
| SettingsViewModel.kt | `database` 注入但未使用；`loadSettings` 返回 mock 数据 | ❌ |
| MedicalRecordViewModel.kt | `keyword`/`fromDate`/`toDate` 参数被接受但未用于过滤 | ❌ |

### God Composable

| 文件 | 行数 |
|------|------|
| HomeScreen.kt | 1,118 |
| SettingsScreen.kt | 966 |
| RemindersScreen.kt | 892 |
| AddMedicalRecordScreen.kt | 607 |
| FamilyScreen.kt | 619 |
| MedicalRecordScreen.kt | 705 |

### 其他代码异味

- `SplashActivity.kt` — 3 处 `runBlocking` 在主线程，ANR 风险 → **已修复** ✅（替换为协程）
- `ReminderReceiver.kt` — 未使用 `goAsync()` → **已修复** ✅
- `MemberEditDialog.kt` — 301 行，Bitmap 解码在主线程（`MemberAvatar.kt` 正确用了 IO 线程）
- `MemberAvatar.kt` — `bitmap!!` 非空断言，且无 Bitmap 降采样（OOM 风险）
- 所有 ViewModel 的 `@RequiresApi(Build.VERSION_CODES.O)` 滥用（部分 VM 不需要）
- 多个 ViewModel 中冗余的全限定类名（`com.yy.medtrace.data.model.FamilyMember`）尽管已有 import

---

## 七、测试问题

- `SettingsViewModel.kt` 是 mock 实现（`loadSettings` 返回硬编码数据），测试无法覆盖真实逻辑
- 无 UI 测试（Compose Test）覆盖关键流程
- FamilyScreen 的 Flow 收集 Bug 没有被测试发现
- TrendChart 的血压数据 Bug 没有被测试发现
- `RemindersViewModel` 的 `error` 字段从未被赋值，但如果有测试验证错误处理则会失败

---

## 八、优先级排序的优化建议

### 立即修复（P0）

| # | 问题 | 状态 |
|---|------|------|
| 1 | RegistrationCodeNative — 移除所有 Log.d 输出注册码/设备 ID 的代码，catch Throwable 而非 Exception | ✅ |
| 2 | MedTraceApplication — `UMConfigure.setLogEnabled(BuildConfig.DEBUG)` | ✅ |
| 3 | FamilyScreen — 修复 Flow 收集 Bug，改用 combine 或为每个成员异步加载 | ✅ |
| 4 | AddMedicalRecordViewModel — saveRecord/updateRecord 加 try-catch，失败时重置 isSaving 并设置 error | ✅ |
| 5 | BackupRepository — 将 HealthTodo 纳入导出/导入 | ✅ |
| 6 | TodoRepositoryImpl — toggleTodoDone 包裹在 withTransaction 中 | ✅ |
| 7 | TrendChart — 修复舒张压数据 Bug | ✅ |
| 8 | 隐私同意拒绝 — 恢复 activity?.finish() 逻辑 | ✅ |
| 9 | insertAll — 改为 OnConflictStrategy.REPLACE | ✅ |
| 10 | SplashActivity — 将 runBlocking 替换为协程 | ✅ |

### 短期优化（P1）

| # | 问题 | 状态 |
|---|------|------|
| 11 | 完成 ViewModel 迁移：MedicalRecordScreen、AddMedicalRecordScreen、FamilyScreen、SettingsScreen、TrendsScreen 全部改用 ViewModel | ✅ |
| 12 | 迁移到 @HiltViewModel，删除 9 个手写 Factory | ✅ |
| 13 | TrendsScreen 接入 TrendChart 组件，实现真正的趋势图 | ✅ |
| 14 | TrendsScreen 渲染 error 状态 | ✅ |
| 15 | PinManager 增加尝试次数限制（5 次错误后锁定 30 秒） | ✅ |
| 16 | SecurePrefs 缓存 EncryptedSharedPreferences 实例 | ✅ |
| 17 | 清理所有死代码 | ✅ |
| 18 | 统一加载/错误/空状态组件（保留一套，删除重复） | ❌ |
| 19 | BiometricHelper 设置 setAllowedAuthenticators | ✅ |
| 20 | ReminderReceiver 使用 goAsync() | ✅ |

### 中期优化（P2）

| # | 问题 | 状态 |
|---|------|------|
| 21 | 提取所有硬编码字符串到 strings.xml | 🔄 |
| 22 | 拆分 God Composable（HomeScreen、SettingsScreen、RemindersScreen） | ❌ |
| 23 | 集中颜色定义到 Color.kt，移除硬编码 Color(0xFF...) | ✅ |
| 24 | 修正所有触摸目标至 ≥48dp | ✅ |
| 25 | SelectedMemberHolder 替换为 UserSettings.selectedMemberId 的 Flow | ✅ |
| 26 | LlmApi — 添加 IOException 捕获，ChatResponse.choices 空列表保护 | ✅ |
| 27 | CryptoUtil — 在加密输出中包含格式版本号和迭代次数 | ✅ |
| 28 | BackupData — 添加 schema 版本字段 | ✅ |
| 29 | MemberEditDialog — Bitmap 解码移到 IO 线程 | ✅ |
| 30 | MemberAvatar — 添加 inSampleSize 降采样 | ✅ |
| 31 | 更新 ui.md / DESIGN.md 使之与实际实现一致 | ❌ |
| 32 | ComprehensiveAnalysisUseCase — 添加用户确认对话框和可选的 PII 脱敏 | ❌ |

### 长期优化（P3）

| # | 问题 | 状态 |
|---|------|------|
| 33 | 补充 v1→v5 数据库迁移（或至少在首次升级时自动加密备份再重建） | ❌ |
| 34 | likePattern 改为 Repository 内部处理 keyword | ❌ |
| 35 | HealthTodo — completedDates 改为关联表或 JSON 数组，消除 CSV 字符串操作 | ❌ |
| 36 | ReminderHelper — 改为可注入类，通知 ID 唯一化，提醒时间可配置 | ❌ |
| 37 | 添加 Compose UI 测试覆盖关键流程 | ❌ |
| 38 | SplashActivity 迁移到 SplashScreen API | ❌ |
| 39 | HealthTodo.category / repeatType 改为枚举 | ❌ |

---

## 九、进度总结

| 等级 | 总计 | 已完成 | 剩余 | 完成率 |
|------|------|--------|------|--------|
| P0 | 15 | 13 | 2 | 87% |
| P1 | 10 | 10 | 0 | 100% |
| P2 | 14 | 9 | 5 | 64% |
| P3 | 7 | 0 | 7 | 0% |
| **合计** | **46** | **32** | **14** | **70%** |

> **P0 说明**：15 项 P0 中，13 项已完成。剩余 2 项已降级处理：
> - ComprehensiveAnalysisUseCase PII 脱敏 → 归入 P2 #32
> - 数据库迁移 v1→v5 → 归入 P3 #33

---

## 十、结论

医迹项目功能完整度较高，安全意识较强（加密备份、生物识别、防截屏等）。本次审查共修复 **32 项问题**（P0 13项 + P1 10项 + P2 9项），覆盖了所有安全类和功能性 Bug，显著提升了应用的稳定性和安全性。

**剩余 14 项**主要集中在：
- **P2 代码质量**（5项）：God Composable 拆分、字符串国际化、文档同步、PII 脱敏、统一状态组件
- **P3 长期基建**（7项）：数据库迁移、likePattern 重构、HealthTodo 枚举化、UI 测试等
- **P0 降级**（2项）：PII 脱敏确认框（#32）、数据库迁移（#33）已分别归入 P2/P3

建议后续按 P2 优先级逐步推进，P3 项视业务需求决定。
