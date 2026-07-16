plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.yy.chiyaole"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yy.chiyaole"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "v1.0.0"

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
            // 启用混淆
            isMinifyEnabled = true
            // 启用资源压缩
            isShrinkResources = true
            // 启用代码优化
            isDebuggable = false
            // 启用 R8 完全模式
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 配置签名
            signingConfig = signingConfigs.getByName("release")
        }
        
        debug {
            // 调试版本不启用混淆
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    
    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // WorkManager for notifications
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.work:work-runtime:2.9.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // LLM (AI extraction): Retrofit + kotlinx.serialization + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}