# Onboarding 与缺陷修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增可跳过的首启引导页，并修复两个缺陷（Room 迁移清空老数据、LlmApi 每次分析新建 Retrofit 实例）。

**Architecture:** 保持现有手动管理架构（无 Hilt/ViewModel）。引导页用本地加密 SharedPreferences 标记 `onboarding_done`，Splash 结束后按标记分流到 `onboarding` 或 `home` 路由；LlmApi 改为按 baseUrl 缓存的单例；AppDatabase 移除 destructive migration fallback。

**Tech Stack:** Kotlin + Jetpack Compose (Material3) + Room + Retrofit2/OkHttp3 + EncryptedSharedPreferences + androidx.security:crypto。

## Global Constraints

- 包名 `com.yy.chiyaole`，debug appId `com.yy.chiyaole.debug`。
- 保持手动架构：各 screen 直接收 `AppDatabase` + `NavController`，不引入 Hilt/ViewModel。
- 视觉遵循已落地「现代精致风」：teal 主色、大圆角、柔和阴影、渐变顶栏（`ui/theme/Design.kt` 的 `PrimaryGradient`）。
- 构建：`export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home && export ANDROID_HOME=/usr/local/share/android-sdk && rtk gradlew :app:assembleDebug`。
- 安装/启动：`adb -s 127.0.0.1:6555 install -r -g app/build/outputs/apk/debug/app-debug.apk`；`adb -s 127.0.0.1:6555 shell am start -n com.yy.chiyaole.debug/com.yy.chiyaole.MainActivity`；清数据 `adb -s 127.0.0.1:6555 shell pm clear com.yy.chiyaole.debug`。
- 本次**不做** i18n、无障碍深度优化、用药提醒、家庭成员权限、CI。
- LlmApi 已有 300s 读超时配置，需在新构建逻辑中保留。

---

## File Structure

- 新增 `data/settings/OnboardingStore.kt`：封装 `onboarding_done` 读写（复用 `SyncSettingsStore` 的 EncryptedSharedPreferences 创建方式）。
- 新增 `ui/screens/OnboardingScreen.kt`：3 屏引导页 Composable，含指示点、跳过、开始使用。
- 修改 `ui/screens/SplashScreen.kt`：`navigate("home")` 改为先读 `OnboardingStore.isDone()` 决定去向。
- 修改 `MainActivity.kt`：`NavHost` 注册 `composable("onboarding")`。
- 修改 `data/AppDatabase.kt`：删除 `fallbackToDestructiveMigration()` 与注释掉的 MIGRATION_1_2/2_3。
- 修改 `data/llm/LlmApi.kt`：companion 内加 `ConcurrentHashMap` 缓存，`create` 改为 `computeIfAbsent`。
- 修改/新增 `data/llm/AnalysisUseCaseTest.kt`：验证同 baseUrl 返回同一实例。

---

### Task 1: OnboardingStore（本地标记）

**Files:**
- Create: `app/src/main/java/com/yy/chiyaole/data/settings/OnboardingStore.kt`

**Interfaces:**
- Consumes: `Context`。
- Produces: `OnboardingStore.isDone(): Boolean`、`OnboardingStore.setDone()`，供 `SplashScreen` 与 `OnboardingScreen` 调用。

- [ ] **Step 1: 创建 OnboardingStore**

```kotlin
package com.yy.chiyaole.data.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnboardingStore(private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "onboarding_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun isDone(): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean(KEY_DONE, false)
    }

    suspend fun setDone() = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_DONE, true).apply()
    }

    companion object {
        private const val KEY_DONE = "onboarding_done"
    }
}
```

- [ ] **Step 2: 构建验证编译通过**

Run: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home && export ANDROID_HOME=/usr/local/share/android-sdk && rtk gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yy/chiyaole/data/settings/OnboardingStore.kt
git commit -m "feat(onboarding): 新增 OnboardingStore 本地完成标记"
```

---

### Task 2: OnboardingScreen（引导页 UI）

**Files:**
- Create: `app/src/main/java/com/yy/chiyaole/ui/screens/OnboardingScreen.kt`

**Interfaces:**
- Consumes: `NavController`、`OnboardingStore`。调用 `OnboardingStore.setDone()` 后 `navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }`。
- Produces: `OnboardingScreen(navController: NavController)` Composable，被 `MainActivity` 的 NavHost 注册。

说明：为避免新增 pager 依赖，用 `mutableStateOf(page)` 控制当前页（0..2），底部指示点 + 「跳过」+ 「下一步/开始使用」按钮。三屏内容固定。

- [ ] **Step 1: 创建 OnboardingScreen**

```kotlin
package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yy.chiyaole.data.settings.OnboardingStore
import com.yy.chiyaole.ui.theme.PrimaryGradient
import kotlinx.coroutines.launch

private data class OnboardPage(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val desc: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { OnboardingStore(context) }
    var page by remember { mutableStateOf(0) }

    val pages = listOf(
        OnboardPage(Icons.Filled.AutoAwesome, "AI 智能识别",
            "拍照或粘贴处方文本，自动提取诊断、用药与检查指标，省去手动录入。"),
        OnboardPage(Icons.Filled.People, "家庭健康管理",
            "为每位家人建立健康档案，归类历次就诊记录与注意事项。"),
        OnboardPage(Icons.Filled.ShowChart, "趋势与解读",
            "检查指标自动成图，AI 给出趋势解读与健康建议，异常一目了然。")
    )

    fun finish() {
        scope.launch {
            store.setDone()
            navController.navigate("home") {
                popUpTo("onboarding") { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(PrimaryGradient)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { finish() }) { Text("跳过", color = Color.White) }
            }

            Spacer(Modifier.weight(1f))

            val current = pages[page]
            Icon(imageVector = current.icon, contentDescription = null,
                tint = Color.White, modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(24.dp))
            Text(text = current.title, color = Color.White, fontSize = 26.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(text = current.desc, color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp))

            Spacer(Modifier.weight(1f))

            Row(modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { i ->
                    Box(modifier = Modifier.size(if (i == page) 24.dp else 8.dp, 8.dp)
                        .background(if (i == page) Color.White else Color.White.copy(alpha = 0.4f), CircleShape))
                }
            }

            Button(
                onClick = { if (page < pages.lastIndex) page++ else finish() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(if (page < pages.lastIndex) "下一步" else "开始使用")
            }
        }
    }
}
```

- [ ] **Step 2: 构建验证编译通过**

Run: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home && export ANDROID_HOME=/usr/local/share/android-sdk && rtk gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yy/chiyaole/ui/screens/OnboardingScreen.kt
git commit -m "feat(onboarding): 新增 3 屏首启引导页 UI"
```

---

### Task 3: 接入路由（Splash + MainActivity）

**Files:**
- Modify: `app/src/main/java/com/yy/chiyaole/ui/screens/SplashScreen.kt`
- Modify: `app/src/main/java/com/yy/chiyaole/MainActivity.kt`

**Interfaces:**
- Consumes: `OnboardingStore.isDone()`、`OnboardingScreen(navController)`。
- Produces: 无（终态接入）。

- [ ] **Step 1: 修改 SplashScreen 导航逻辑**

在 `SplashScreen.kt` 顶部 import 区确保包含：
```kotlin
import androidx.compose.ui.platform.LocalContext
import com.yy.chiyaole.data.settings.OnboardingStore
```
（若文件中未声明 `context`，在 `SplashScreen` 函数体内 `Box {` 之前加 `val context = LocalContext.current`。）

将现有 `LaunchedEffect` 内：
```kotlin
        delay(2500)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
```
替换为：
```kotlin
        delay(2500)
        val done = OnboardingStore(context).isDone()
        navController.navigate(if (done) "home" else "onboarding") {
            popUpTo("splash") { inclusive = true }
        }
```

- [ ] **Step 2: MainActivity 注册 onboarding 路由**

在 `MainActivity.kt` 的 `NavHost` 中，`composable("splash")` 块之后追加（确保 `import com.yy.chiyaole.ui.screens.OnboardingScreen` 已存在，同包 `ui.screens.*` 一般已导入）：
```kotlin
            composable("onboarding") {
                OnboardingScreen(navController)
            }
```

- [ ] **Step 3: 构建并安装运行验证**

Run:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home && export ANDROID_HOME=/usr/local/share/android-sdk && rtk gradlew :app:assembleDebug && adb -s 127.0.0.1:6555 install -r -g app/build/outputs/apk/debug/app-debug.apk && adb -s 127.0.0.1:6555 shell pm clear com.yy.chiyaole.debug && adb -s 127.0.0.1:6555 shell am start -n com.yy.chiyaole.debug/com.yy.chiyaole.MainActivity
```
Expected: 首次启动显示引导页；点「跳过」或「开始使用」进入首页；再次 `am start` 不再出现引导页（标记已写）。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yy/chiyaole/ui/screens/SplashScreen.kt app/src/main/java/com/yy/chiyaole/MainActivity.kt
git commit -m "feat(onboarding): Splash 按标记分流到引导页或首页"
```

---

### Task 4: 修复 Room 迁移清空数据缺陷

**Files:**
- Modify: `app/src/main/java/com/yy/chiyaole/data/AppDatabase.kt`

**Interfaces:**
- Consumes: 无。
- Produces: 无（仅移除 destructive fallback，保留 `MIGRATION_6_7`、`MIGRATION_7_8`）。

- [ ] **Step 1: 删除注释掉的 MIGRATION_1_2 / MIGRATION_2_3**

在 `AppDatabase.kt` 的 `companion object` 内，删除从 `//        private val MIGRATION_1_2 = ...` 到对应 `//        }` 结束（含 `MIGRATION_2_3`）的全部注释块（即文件第 47–101 行区间的注释）。

- [ ] **Step 2: 移除 fallbackToDestructiveMigration()**

将 `getDatabase()` 构建链：
```kotlin
                )
//                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
```
改为：
```kotlin
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
```

- [ ] **Step 3: 构建验证编译通过**

Run: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home && export ANDROID_HOME=/usr/local/share/android-sdk && rtk gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL（且不再有 destructive fallback）。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yy/chiyaole/data/AppDatabase.kt
git commit -m "fix(db): 移除 fallbackToDestructiveMigration，保留已知迁移避免老数据丢失"
```

---

### Task 5: 修复 LlmApi 每次分析新建 Retrofit 实例

**Files:**
- Modify: `app/src/main/java/com/yy/chiyaole/data/llm/LlmApi.kt`
- Modify/Create: `app/src/main/java/com/yy/chiyaole/data/llm/LlmApiCacheTest.kt`（位于 `src/test`）

**Interfaces:**
- Consumes: 无（仅内部缓存）。
- Produces: `LlmApi.create(baseUrl: String): LlmApi` 行为不变（签名不变），但同 baseUrl 返回同一缓存实例；超时（300s 读）保留。

- [ ] **Step 1: 写失败测试（验证同 baseUrl 返回同一实例）**

新建 `app/src/test/java/com/yy/chiyaole/data/llm/LlmApiCacheTest.kt`：
```kotlin
package com.yy.chiyaole.data.llm

import org.junit.Assert.assertSame
import org.junit.Test

class LlmApiCacheTest {
    @Test
    fun `create with same baseUrl returns same instance`() {
        val a = LlmApi.create("https://api.example.com/v1")
        val b = LlmApi.create("https://api.example.com/v1")
        assertSame(a, b)
    }

    @Test
    fun `create with different baseUrl returns different instance`() {
        val a = LlmApi.create("https://api.example.com/v1")
        val b = LlmApi.create("https://other.example.com/v1")
        org.junit.Assert.assertNotSame(a, b)
    }
}
```
注：测试依赖网络构建但不发起请求；若 `LlmApi.create` 内部 `normalizeBaseUrl()` 改变 URL 字符串，测试用相同输入即可。

- [ ] **Step 2: 运行测试确认失败（改前 LlmApi 每次新建，assertSame 必失败）**

Run: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home && export ANDROID_HOME=/usr/local/share/android-sdk && rtk gradlew :app:testDebugUnitTest --tests "com.yy.chiyaole.data.llm.LlmApiCacheTest"`
Expected: 至少一个测试 FAIL（`assertSame` 不成立）。

- [ ] **Step 3: 改写 LlmApi 增加按 baseUrl 缓存**

将 `LlmApi.kt` 的 `companion object` 改为：
```kotlin
    companion object {
        private val cache = java.util.concurrent.ConcurrentHashMap<String, LlmApi>()

        fun create(baseUrl: String): LlmApi {
            val key = baseUrl.normalizeBaseUrl()
            return cache.computeIfAbsent(key) { build(key) }
        }

        private fun build(baseUrl: String): LlmApi {
            val json = Json { ignoreUnknownKeys = true }
            val clientBuilder = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
            if (BuildConfig.DEBUG) {
                clientBuilder.addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
                )
            }
            val client = clientBuilder.build()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build().create(LlmApi::class.java)
        }
    }
```
（`normalizeBaseUrl()` 为 `AnalysisUseCase` 中已存在的扩展函数，作用于 String；若其可见性为 `private`，需改为 `internal` 或 `public` 以便 `LlmApi` 复用。确认该扩展定义位置后相应调整可见性。）

- [ ] **Step 4: 运行测试确认通过**

Run: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home && export ANDROID_HOME=/usr/local/share/android-sdk && rtk gradlew :app:testDebugUnitTest --tests "com.yy.chiyaole.data.llm.LlmApiCacheTest"`
Expected: 两个测试均 PASS。

- [ ] **Step 5: 全量构建验证**

Run: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home && export ANDROID_HOME=/usr/local/share/android-sdk && rtk gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yy/chiyaole/data/llm/LlmApi.kt app/src/test/java/com/yy/chiyaole/data/llm/LlmApiCacheTest.kt
git commit -m "fix(llm): LlmApi 按 baseUrl 缓存 Retrofit 实例，避免每次分析新建"
```

---

