plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.yy.medtrace"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yy.medtrace"
        minSdk = 24
        targetSdk = 35
        // versionCode 约定：每个 minor 版本 +1（v2.0.0 = 10）。
        // versionCode = 10 + minor * 1 + patch
        versionCode = 31
        versionName = "v3.9.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema location
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
                )
            }
        }

        // 启用 R8 完全模式
        ndk {
            debugSymbolLevel = "FULL"
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        
        // NDK CMake
        externalNativeBuild {
            cmake {
                cppFlags += ""
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    // 配置签名信息 - 移到 buildTypes 之前
    // 密码仅从本地 keystore.properties（已 gitignore）或环境变量读取，不写死在仓库中
    val keyPropsFile = rootProject.file("keystore.properties")
    val keyProps = if (keyPropsFile.exists()) {
        keyPropsFile.readLines().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
            val eq = trimmed.indexOf('=')
            if (eq < 0) return@mapNotNull null
            trimmed.substring(0, eq).trim() to trimmed.substring(eq + 1).trim()
        }.toMap()
    } else {
        emptyMap()
    }
    val getSecret: (envKey: String, propKey: String) -> String? = { envKey, propKey ->
        System.getenv(envKey) ?: keyProps[propKey]
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/release.keystore")
            storePassword = getSecret("CHIYAOLE_KEYSTORE_PASSWORD", "storePassword")
                ?: error("未配置签名密码：请在 keystore.properties 设置 storePassword，或设置环境变量 CHIYAOLE_KEYSTORE_PASSWORD")
            keyAlias = getSecret("CHIYAOLE_KEY_ALIAS", "keyAlias") ?: "chiyaole_key"
            keyPassword = getSecret("CHIYAOLE_KEY_PASSWORD", "keyPassword")
                ?: error("未配置签名密码：请在 keystore.properties 设置 keyPassword，或设置环境变量 CHIYAOLE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            // 发布版：VIP功能需要购买
            buildConfigField("boolean", "VIP_ENABLED", "false")
        }
        
        debug {
            // 调试版本不启用混淆；与 release 共用 applicationId，避免同机双实例/数据割裂
            isMinifyEnabled = false
            isDebuggable = true
            // 开发版：VIP功能免费
            buildConfigField("boolean", "VIP_ENABLED", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    // 配置 lint
    lint {
        checkReleaseBuilds = true
        abortOnError = true
        disable += "MissingTranslation"
    }
    
    // 配置 packagingOptions
    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
    
    // NDK CMake
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    
    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")
    
    // WorkManager for notifications
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Biometric (fingerprint / face unlock)
    implementation(libs.androidx.biometric)

    // EncryptedSharedPreferences for PIN hash storage
    implementation(libs.androidx.security.crypto)
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    
    // Hilt
    val hiltVersion = "2.50"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Mockk for testing
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    
    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // LLM (AI extraction): Retrofit + kotlinx.serialization + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation("androidx.paging:paging-runtime-ktx:3.3.0")
    implementation("androidx.paging:paging-compose:3.3.0")
    
    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "androidx.fragment") {
                useVersion("1.8.8")
                because("旧版 fragment:1.2.5 的 FragmentActivity 对 requestCode 做 16 位静态校验，与 activity 1.10.x 生成的大 requestCode 冲突导致 startActivityForResult 崩溃")
            }
        }
    }
}

androidComponents {
    onVariants(selector().withName("debug")) {
        // 调试包文件名可在此自定义；保持默认输出，避免配置错误
    }
}