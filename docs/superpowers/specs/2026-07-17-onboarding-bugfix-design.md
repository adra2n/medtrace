# 医迹 App — Onboarding 与缺陷修复设计文档

日期：2026-07-17
分支：`feature/ai-health-trends`（基于 `v1.4.0` 之后）

## 范围

本次只做三件事，均为视觉层/架构层之外的小幅改动，不引入新架构（保持手动管理、无 Hilt/ViewModel 现状）：

1. **首启引导页（Onboarding）**：轻量、可跳过、本地标记。
2. **修复 Room 迁移会清空老用户数据的缺陷**。
3. **修复 `AnalysisUseCase` 每次分析新建 Retrofit 实例、无缓存的问题**。

本次**不做**：国际化（i18n）、无障碍（a11y 深度优化）——已与用户确认排除。

---

## 1. 首启引导页（Onboarding）

### 目标
首次启动 App 时，用 1–3 屏介绍核心能力，降低新用户认知成本；可跳过；之后不再出现。

### 触发与流转
- 复用现有 `SplashScreen` 的零闪烁体验：`SplashScreen` 结束后，根据本地标记决定去向。
- 新增标记 `onboarding_done`（见下），存于加密 SharedPreferences。
- 路由逻辑（`MainActivity` 的 `NavHost` 或 `SplashScreen` 内）：
  - 未完成引导 → 导航到新增 `onboarding` 路由（引导页）。
  - 已完成 → 导航到 `home`。
- 引导页最后一屏「开始使用」或任意屏「跳过」→ 写 `onboarding_done = true` → 导航到 `home`（pop 掉 splash/onboarding）。

### 数据存储
- 新增 `data/settings/OnboardingStore.kt`，复用 `SyncSettingsStore` 的 `EncryptedSharedPreferences` 创建方式（`AES256_SIV` / `AES256_GCM`）。
- 提供 `isDone(): Boolean` 与 `setDone()`。
- 选型理由：与成员数据解耦，轻量、即刻生效，不污染 `UserSettings` 实体。

### UI（遵循已落地的「现代精致风」设计语言）
- 新增 `ui/screens/OnboardingScreen.kt`，使用 `ui/theme/Design.kt` 的 `PrimaryGradient` 背景（与 Splash 一致）。
- 横向 `HorizontalPager` 3 屏，每屏：图标（Material Icon）+ 标题 + 一句说明：
  1. AI 智能识别：拍照 / 粘贴文本，自动提取诊断、用药与检查指标。
  2. 家庭健康管理：为每位家人建立档案，归类历次就诊记录。
  3. 趋势与解读：指标自动成图，AI 给出趋势解读与健康建议。
- 底部：`Pager` 指示点 + 「跳过」（右上或底部左）+ 末屏「开始使用」按钮（渐变主色）。
- 视觉：大圆角卡片、柔和阴影、teal 主色，与首页/启动页一致。
- 不做登录、不做初始成员/LLM 配置引导（保持轻量）。

### 接入点
- `MainActivity.kt` 的 `NavHost` 增加 `composable("onboarding") { OnboardingScreen(navController) }`。
- `SplashScreen.kt`：将现有的固定 `navController.navigate("home")` 改为先读 `OnboardingStore.isDone()`，决定 `home` 或 `onboarding`。

---

## 2. 修复 Room 迁移清空数据缺陷

### 问题
`AppDatabase.kt` 当前：
- `MIGRATION_1_2` / `MIGRATION_2_3` 被注释（且引用已废弃的 `medication_reminders` 等表，属已放弃的提醒功能）。
- `.fallbackToDestructiveMigration()` 在找不到对应迁移版本时**直接删表重建**，导致停留在 v1–v5 的老用户升级到 v8 时数据全丢。

### 方案（已与用户确认）
- **移除 `fallbackToDestructiveMigration()`。**
- 仅保留已知且正确的 `MIGRATION_6_7`、`MIGRATION_7_8`（均为 `ADD COLUMN`，不丢数据）。
- 不补 v1–v5 的推测性迁移：git 历史中 v1–v5 的 schema 已丢失，无法正确还原；v1–v5 为内部预发布版本，几乎无真实用户。若此类极老用户升级遇 `IllegalStateException`（无迁移），可凭既有加密备份（Gist/SAF）恢复。
- 保留 `exportSchema = false`（现状）。

### 改动
- `AppDatabase.kt`：`getDatabase()` 构建链中删除 `.fallbackToDestructiveMigration()`；删除已注释的 `MIGRATION_1_2`/`2_3` 代码块（清理技术债）。
- 风险：仅影响 v6 以下且仍在使用的设备；当前线上版本为 v8，正常用户走 6_7/7_8 无影响。

---

## 3. 修复 `AnalysisUseCase` Retrofit 实例复用

### 问题
`LlmApi.kt` 的 `companion object.create(baseUrl)` 每次调用都新建 `OkHttpClient` + `Retrofit`，无缓存。虽已在 `create` 内配置 300s 读超时，但实例未复用，且 `AnalysisUseCase` 每次分析都新建，浪费资源、无统一连接池。

### 方案（已与用户确认）
- 将 `LlmApi` 改为**按 baseUrl 缓存的单例**：内部用 `ConcurrentHashMap<String, LlmApi>`。
- `create(baseUrl)` 命中缓存直接返回；未命中则构建（含 300s 读超时、30s 连接、120s 写超时、DEBUG 日志拦截器）并存入缓存。
- 超时配置只在新构建时发生一次，线程安全由 `ConcurrentHashMap` 保证（computeIfAbsent）。

### 改动
- `LlmApi.kt`：`companion object` 内增加 `private val cache = ConcurrentHashMap<String, LlmApi>()`；`create` 改为 `cache.computeIfAbsent(baseUrl.normalizeBaseUrl()) { build(...) }`，把现有构建逻辑抽到 `build()`。
- 调用方（`AnalysisUseCase`）无需改动——`LlmApi.create(base)` 签名不变。

---

## 测试与验证

- **构建**：`export JAVA_HOME=...temurin-21... && export ANDROID_HOME=... && rtk gradlew :app:assembleDebug` 通过。
- **安装运行**：装到模拟器 `127.0.0.1:6555`，`pm clear` 后首启应见引导页；点「跳过」/「开始使用」进首页；再次启动不再出现引导页。
- **迁移**：因无法构造 v1–v5 老数据，仅做静态确认（移除 destructive fallback、保留 6_7/7_8 编译通过）；v8 用户数据不受影响。
- **Retrofit 复用**：加一个轻量单测或在 `AnalysisUseCase` 单测中验证两次 `LlmApi.create(sameUrl)` 返回同一实例（`===`）。
- 沿用现有 5 个单测不被破坏。

## 文件改动清单

- 新增：`ui/screens/OnboardingScreen.kt`
- 新增：`data/settings/OnboardingStore.kt`
- 改：`MainActivity.kt`（NavHost 注册 onboarding）
- 改：`SplashScreen.kt`（按标记决定去向）
- 改：`data/AppDatabase.kt`（移除 destructive fallback + 清理注释迁移）
- 改：`data/llm/LlmApi.kt`（按 baseUrl 缓存单例）

## 明确不做

- 国际化（i18n）、无障碍深度优化、用药提醒、家庭成员权限、CI、全局搜索 —— 均不在本次范围。
