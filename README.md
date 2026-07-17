# 医迹 (ChiYaoLe)

> 个人及家庭成员的医学检查记录与用药管理工具，Android 原生应用，Jetpack Compose + Room 构建。

医迹帮助你把分散在各处的就诊记录、检查报告、用药方案集中管理，并通过 AI 分析把非结构化的病历文本 / 照片转化为结构化数据，方便长期追踪与复诊参考。

> 注：本仓库包名仍为历史值 `com.yy.chiyaole`（未改名），应用对外显示名为「医迹」。

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

## 目录结构（核心）

```
app/src/main/java/com/yy/chiyaole/
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
