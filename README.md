# 医迹 (ChiYaoLe)

> 个人及家庭成员的医学检查记录与用药管理工具，Android 原生应用，Jetpack Compose + Room 构建。

医迹帮助你把分散在各处的就诊记录、检查报告、用药方案集中管理，并通过 AI 分析把非结构化的病历文本 / 照片转化为结构化数据，方便长期追踪与复诊参考。

> 注：本仓库包名为 `com.yy.medtrace`，应用对外显示名为「医迹」。

## 功能特性

- **家庭成员管理**：维护成员档案（关系、性别、生日、血型、过敏史、慢性病、用药备注等），可设默认成员。
- **病历记录**：记录诊断、发病时间、就诊医院、用药项、剂量、频次与备注；支持从文本 / 图片经 AI 解析自动填充。
- **AI 分析**：接入大模型（LLM）对病历文本或检查报告照片进行结构化分析（`data/llm`）。
- **数据安全**：
  - 应用锁：生物识别（指纹 / 人脸）+ 6 位 PIN 备用密码，支持自动锁定（立即 / 1 分钟 / 5 分钟）与阻止截屏录屏。
  - 数据备份与恢复：通过系统文件选择器（SAF，无需存储权限）导出 / 导入 JSON 备份。
  - AES-256-GCM 加密导出：备份可设密码加密（PBKDF2 派生密钥）。
  - GitHub Gist 同步：加密备份可同步到私有 Gist，并支持从 Gist 恢复。
  - 本地敏感数据（Token、加密密码、PIN 哈希）使用 `EncryptedSharedPreferences` 存储；`allowBackup=false` 防止 adb 备份提取。
- **自适应图标**：青绿底 + 医疗十字 / 心电波形 / 病历横线。

## 技术栈

- 语言：Kotlin
- UI：Jetpack Compose (Material 3)，Compose BOM 2024.04.01
- 架构：手动管理（各 screen 直接持有 `AppDatabase` + `NavController`，无 Hilt / ViewModel 框架）
- 持久化：Room + SQLite（SQLCipher 非默认；加密层在备份导出与偏好设置）
- 安全：`androidx.biometric` 1.2.0-alpha05、`androidx.security:security-crypto` 1.1.0-alpha06（MasterKey + Tink）
- 异步：Kotlin 协程

## 构建要求

- JDK 21（Gradle 8.9 / AGP 8.x / Kotlin 2.0）
- Android SDK（compileSdk / targetSdk 35，minSdk 24）
- 本地签名配置：`keystore.properties`（已 gitignore）与 `keystore/release.keystore`（本地，不入库）

## 构建与运行

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
export ANDROID_HOME=/usr/local/share/android-sdk

# 调试包
./gradlew :app:assembleDebug

# 发布包（需本地 keystore.properties / release.keystore）
./gradlew :app:assembleRelease
```

产物分别位于：

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

安装到已连接设备：

```bash
adb -s <device-id> install -r -g app/build/outputs/apk/release/app-release.apk
```

> 注意：`keystore/` 与 `keystore.properties` 已在 `.gitignore` 中，请勿提交签名密钥与密码。

## 下载

签名发布包在 [GitHub Releases](https://github.com/adra2n/medtrace/releases) 页面，最新稳定版为 **v3.4.0**（`MedTrace/MedTrace-v3.4.0.apk`，见发布仓库 [adra2n/product](https://github.com/adra2n/product)）。

安装到已连接设备：

```bash
adb -s <device-id> install -r -g MedTrace-v3.4.0.apk
```

## 目录结构（核心）

```
app/src/main/java/com/yy/medtrace/
├── data/
│   ├── model/        实体：FamilyMember / MedicalRecord / UserSettings ...
│   ├── dao/          Room DAO
│   ├── backup/       BackupRepository / CryptoUtil / GistSync / BackupData
│   ├── security/     PinManager / SecurePrefs / BiometricHelper
│   ├── settings/     SyncSettingsStore / SecuritySettingsStore / LlmSettingsStore
│   ├── llm/          LlmApi / AnalysisUseCase（AI 病历分析）
│   ├── converter/    Room 类型转换器（LocalDate / LocalDateTime / 列表等）
│   └── AppDatabase.kt
├── ui/
│   ├── screens/      Home / Family / MedicalRecord / AddMedicalRecord / Settings / Lock / About / Splash
│   ├── components/  复用 Compose 组件
│   ├── theme/       主题与配色
│   └── state/       界面状态
├── worker/          后台任务（如通知）
├── MainActivity.kt  导航与中央应用锁门控
└── ChiyaoleApplication.kt
```

## 版本里程碑

- **v3.4.0**（versionCode 23，2026-07-22）
  - 新增：友盟 U-AppWin 开屏广告 SDK 集成。
  - 新增：开屏广告加载失败时显示兜底启动页（App图标 + 名称）。
  - 修复：跳过按钮被状态栏遮挡的问题。

- **v3.3.0**（versionCode 22，2026-07-21）
  - 导航栏重构：首页、家人、提醒、我的 4个tab。
  - 新增健康提醒页面（按成员筛选）。
  - 新增个人中心页面（整合设置入口）。
  - 家庭成员页面优化（卡片点击进入、按钮右置）。
  - 今日提醒显示具体待办事项。
  - 统一所有页面成员头像样式。
  - 修复 Hilt 启动崩溃问题。
  - 底部导航栏嵌入页面（非浮动）。
  - GitHub Releases：<https://github.com/adra2n/medtrace/releases/tag/v3.3.0>

- **v3.2.5**（versionCode 20，2026-07-19）
  - 健康待办每日提醒防重改为数据库持久化（`health_todos.notifiedDate` 字段，db v11）：重装 / 清数据后随加密备份恢复保持一致，不再依赖 SharedPreferences 导致漏弹或重复弹。
  - 提醒通知带类型统计：文案区分「服药 N 条 / 复查 N 条」。
  - 设置页「关于」版本号显示 versionCode，便于核对。
  - GitHub Releases：<https://github.com/adra2n/medtrace/releases/tag/v3.2.5>

- **v3.2.3**（versionCode 19，2026-07-19）
  - 精确闹钟权限引导优化：跳转系统设置页前先弹说明对话框，告知用户需开启「精确闹钟」权限及其作用（每天 9 点准时弹出健康待办提醒）；从设置页返回后自动重试一次排程，已授权即立即生效，未授权静默放弃。
  - GitHub Releases：<https://github.com/adra2n/medtrace/releases/tag/v3.2.3>
- **v3.2.2**（versionCode 18，2026-07-19）
  - 修复启动崩溃：Android 12+ 上若「精确闹钟」权限被系统/厂商拒绝，`setExactAndAllowWhileIdle` 抛 `SecurityException` 导致 App 闪退。新增 `canScheduleExactAlarms()` 守卫，未授权时安全跳过排程并引导用户到精确闹钟设置页授权。
  - GitHub Releases：<https://github.com/adra2n/medtrace/releases/tag/v3.2.2>
- **v3.2.1**（versionCode 17，2026-07-19）
  - 首页顶部栏优化：渐变提亮（中青蓝 → 浅青蓝 PrimaryLight），不再发暗；栏高收紧（vertical padding 18dp → 14dp）。
  - GitHub Releases：<https://github.com/adra2n/medtrace/releases/tag/v3.2.1>
- **v3.2.0**（versionCode 16，2026-07-18）
  - 首页排版优化：去独立「家人」标题改为「我的家人」小标题；待办区去右上角「+」、空态加快捷 chip、每条待办左侧成员首字母头像；四宫格去重（医疗记录 / 病历档案 / 健康趋势 / 功能设置）。
  - 新增健康待办每日定时提醒：AlarmManager 每日 9 点后台触发通知（MIUI 前台通知被系统拦截，改用后台触发），含通知权限运行时请求与同日防重。
  - GitHub Releases：<https://github.com/adra2n/medtrace/releases/tag/v3.2.0>
- **v3.1.1**（versionCode 15）：顶部栏去渐变弧线，细节打磨。
- **v3.0.0**（versionCode 14）：健康待办与提醒体系初版。
- **v2.0.0**（versionCode 10，2026-07-17）
  - 重大版本：在 v1.5.x 基础上汇总本轮全部体验与健壮性优化。
  - 首启引导页（3 屏可跳过）+ 现代精致风 UI。
  - 设置页体验：仅保留右上角实心「保存」按钮，保存即返回上一页；深色模式切换即时预览。
  - 选中成员单一来源：首页 / 记录 / 趋势 / 家庭 / 添加记录统一选中成员并持久化，修复「加了记录但概览没变」。
  - 删除家庭成员后选中自动回退（默认成员 / 首个剩余成员），首页自动刷新。
  - AI 分析期间按钮禁用 + 进度提示，避免重复触发。
  - 迁移健壮性：历史迁移（v1–v5）缺失不再崩溃，自动重置并首页常驻提醒从备份恢复。
  - 健康概览 / 趋势空态引导文案（明确「仅手动填处方不会生成指标」）。
  - 记录排序兜底 `COALESCE(onsetTime, ...)`，防止异常排序。
  - 工程：`.DS_Store` 移出版本控制，`versionCode` 严格按 minor 递增。
  - GitHub Releases：<https://github.com/adra2n/chiyaole/releases/tag/v2.0.0>
- **v1.5.1**（versionCode 9，2026-07-17）
  - 工程优化：`.DS_Store` 移出版本控制，避免仓库脏改动。
  - 健壮性：Room 历史迁移（v1–v5）缺失时不再崩溃，自动重置旧库并提示用户从加密备份恢复。
  - 规范：`versionCode` 约定按 minor 严格递增并加注释。
  - GitHub Releases：<https://github.com/adra2n/chiyaole/releases/tag/v1.5.1>
- **v1.5.0**（versionCode 8，2026-07-17）
  - 新增首启引导页（3 屏、可跳过，本地标记 `OnboardingStore`）。
  - 全新「现代精致风」UI（渐变顶栏、白卡浮起、大圆角、柔和阴影、`PrimaryGradient` 共享设计语言）。
  - 修复 Room 迁移会清空老用户数据缺陷：移除 `fallbackToDestructiveMigration()`，仅保留已知 `MIGRATION_6_7` / `MIGRATION_7_8`。
  - 修复 `LlmApi` 每次分析新建 Retrofit 实例：按 `baseUrl` 缓存单例（`ConcurrentHashMap`）。
  - 修复 release 包（R8）下两处序列化崩溃：保留 Gson `TypeToken` 泛型签名、保留 kotlinx.serialization `@Serializable` 类及生成 `Serializer`（健康概览指标解析）。
  - 修复导航与设置页交互：子页面隐藏底部导航栏避免与设置页底栏重叠；设置页仅保留右上角实心「保存」按钮；修复底部标签切换误恢复其他标签页面状态。
  - 新增 AI 健康趋势图（持久化 AI 指标 + 折线趋势图）。
  - GitHub Releases：<https://github.com/adra2n/chiyaole/releases/tag/v1.5.0>
- **v1.4.0**（versionCode 5）：现代精致风 UI 重设计。
- **v1.3.0**：功能迭代。
- **v1.2.0**（versionCode 5）：版本号规范化。
- **v1.1.0**（versionCode 4）：基础家庭成员与病历管理、AI 分析、应用锁与加密备份。
- **v1.0.1**：初始发布。

> 说明：v1.2.0 起 `versionCode` 未按 minor 严格递增（多个 tag 沿用 code 5）；v1.5.0 调整为 code 8。

## 许可

本项目基于 [GNU General Public License v3.0 (GPL-3.0)](LICENSE) 开源（强 copyleft，衍生作品须同样以 GPL-3.0 开源）。

若需将本项目与 AGPL-3.0 程序组合，或对其他许可方式有疑问，请参考 [GNU GPL 说明](https://www.gnu.org/licenses/gpl-3.0.html)。
