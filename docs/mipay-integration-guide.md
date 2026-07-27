# 小米支付SDK集成指南

## 一、准备工作

### 1. 注册小米开发者账号
1. 访问 https://dev.mi.com/
2. 注册账号并完成实名认证
3. 创建应用并获取 APP_ID

### 2. 配置应用内支付
1. 在小米开放平台添加应用
2. 选择"应用内支付"功能
3. 添加商品：
   - 商品ID: `medtrace_unlock_all`
   - 商品名称: 医迹高级版
   - 价格: ¥9.90

### 3. 下载SDK
1. 访问 https://dev.mi.com/console/doc/detail?pId=1588
2. 下载"小米支付SDK"（MiPay SDK）
3. 将下载的aar文件放入 `app/libs/` 目录

---

## 二、代码集成

### 1. 添加依赖（app/build.gradle.kts）

```kotlin
dependencies {
    // 小米支付SDK
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    
    // EncryptedSharedPreferences（用于安全存储）
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

### 2. 配置AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- 网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <application
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.MedTrace">
        
        <!-- 其他Activity -->
        
        <!-- 小米支付Activity -->
        <activity
            android:name="com.xiaomi.mipay.sdk.MiPayActivity"
            android:exported="false"
            android:configChanges="orientation|keyboardHidden|screenSize" />
            
    </application>
</manifest>
```

### 3. 创建MiPayManager（app/src/main/java/com/yy/medtrace/payment/MiPayManager.kt）

```kotlin
package com.yy.medtrace.payment

import android.app.Activity
import android.content.Context
import android.util.Log
import com.xiaomi.mipay.sdk.MiPay
import com.xiaomi.mipay.sdk.MiPayCallback
import com.xiaomi.mipay.sdk.MiPayClient
import com.xiaomi.mipay.sdk.MiPayOrderInfo
import com.yy.medtrace.data.settings.PremiumManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 小米支付管理器
 */
@Singleton
class MiPayManager @Inject constructor(
    private val premiumManager: PremiumManager
) {
    companion object {
        private const val TAG = "MiPayManager"
        
        // TODO: 替换为你的APP_ID
        const val APP_ID = "YOUR_APP_ID"
        
        // 商品信息
        const val PRODUCT_ID = "medtrace_unlock_all"
        const val PRODUCT_NAME = "医迹高级版"
        const val PRODUCT_DESCRIPTION = "解锁所有高级功能"
        const val PRODUCT_PRICE = 990 // 单位：分
    }

    private var miPayClient: MiPayClient? = null
    
    private val _payState = MutableStateFlow<MiPayState>(MiPayState.Idle)
    val payState: StateFlow<MiPayState> = _payState

    /**
     * 初始化小米支付（在Activity的onCreate中调用）
     */
    fun init(activity: Activity) {
        try {
            miPayClient = MiPay.create(activity, APP_ID)
            Log.d(TAG, "小米支付初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "小米支付初始化失败", e)
            _payState.value = MiPayState.Error("支付初始化失败: ${e.message}")
        }
    }

    /**
     * 发起购买
     */
    fun startPurchase() {
        if (premiumManager.isPremiumActive()) {
            _payState.value = MiPayState.AlreadyPurchased
            return
        }

        val client = miPayClient
        if (client == null) {
            _payState.value = MiPayState.Error("支付未初始化")
            return
        }

        _payState.value = MiPayState.Loading

        val orderInfo = MiPayOrderInfo().apply {
            productId = PRODUCT_ID
            productName = PRODUCT_NAME
            productDescription = PRODUCT_DESCRIPTION
            totalAmount = PRODUCT_PRICE
            outTradeNo = "medtrace_${System.currentTimeMillis()}"
        }

        client.pay(orderInfo, object : MiPayCallback {
            override fun onSuccess(orderId: String, receipt: String) {
                Log.d(TAG, "支付成功: orderId=$orderId")
                // 保存购买收据
                premiumManager.savePurchaseReceipt(orderId, receipt)
                _payState.value = MiPayState.Success(orderId)
            }

            override fun onFailure(code: Int, msg: String) {
                Log.e(TAG, "支付失败: code=$code, msg=$msg")
                _payState.value = MiPayState.Error("支付失败: $msg")
            }

            override fun onCanceled() {
                Log.d(TAG, "支付取消")
                _payState.value = MiPayState.Canceled
            }
        })
    }

    /**
     * 恢复购买
     */
    fun restorePurchase() {
        _payState.value = MiPayState.Loading
        
        // 检查本地是否有购买记录
        if (premiumManager.isPremiumActive()) {
            _payState.value = MiPayState.AlreadyPurchased
            return
        }
        
        // TODO: 调用小米支付SDK查询历史订单
        // miPayClient?.queryOrder(...)
        
        _payState.value = MiPayState.Error("未找到购买记录")
    }

    /**
     * 处理支付回调（在Activity的onActivityResult中调用）
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        miPayClient?.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _payState.value = MiPayState.Idle
    }
}

/**
 * 小米支付状态
 */
sealed class MiPayState {
    data object Idle : MiPayState()
    data object Loading : MiPayState()
    data object AlreadyPurchased : MiPayState()
    data class Success(val orderId: String) : MiPayState()
    data class Error(val message: String) : MiPayState()
}
```

### 4. 更新PremiumManager（app/src/main/java/com/yy/medtrace/data/settings/PremiumManager.kt）

```kotlin
package com.yy.medtrace.data.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 高级功能管理器
 * 使用EncryptedSharedPreferences安全存储购买状态
 */
@Singleton
class PremiumManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "premium_prefs"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * 是否已解锁高级功能
     */
    fun isPremiumActive(): Boolean {
        return encryptedPrefs.getBoolean("is_premium", false)
    }

    /**
     * 保存购买收据
     */
    fun savePurchaseReceipt(orderId: String, receipt: String) {
        encryptedPrefs.edit()
            .putBoolean("is_premium", true)
            .putString("receipt", receipt)
            .putString("order_id", orderId)
            .putLong("purchase_time", System.currentTimeMillis())
            .apply()
    }

    /**
     * 获取购买收据
     */
    fun getPurchaseReceipt(): String? {
        return encryptedPrefs.getString("receipt", null)
    }

    /**
     * 获取订单号
     */
    fun getOrderId(): String? {
        return encryptedPrefs.getString("order_id", null)
    }

    /**
     * 获取购买时间
     */
    fun getPurchaseTime(): Long {
        return encryptedPrefs.getLong("purchase_time", 0)
    }

    /**
     * 验证购买状态
     */
    fun verifyPurchase(): Boolean {
        val receipt = getPurchaseReceipt()
        return receipt != null && receipt.isNotEmpty()
    }

    /**
     * 清除购买状态（用于测试）
     */
    fun clearPurchase() {
        encryptedPrefs.edit().clear().apply()
    }

    /**
     * 检查是否可以添加更多成员
     */
    fun canAddMember(currentCount: Int): Boolean {
        if (isPremiumActive()) return true
        return currentCount < 1 // 免费版最多1人
    }

    /**
     * 检查今日AI识别次数是否用完
     */
    fun canUseAiToday(todayCount: Int): Boolean {
        if (isPremiumActive()) return true
        return todayCount < 3 // 免费版每天3次
    }
}
```

### 5. 更新PaymentManager（app/src/main/java/com/yy/medtrace/payment/PaymentManager.kt）

```kotlin
package com.yy.medtrace.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.yy.medtrace.data.settings.PremiumManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 支付管理器
 */
@Singleton
class PaymentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val premiumManager: PremiumManager,
    private val miPayManager: MiPayManager
) {
    companion object {
        const val CONTACT_EMAIL = "cljkle@163.com"
        const val PRODUCT_PRICE_DISPLAY = "¥9.90"
    }

    private val _payState = MutableStateFlow<PayState>(PayState.Idle)
    val payState: StateFlow<PayState> = _payState

    /**
     * 发起购买
     */
    fun startPurchase(activity: Activity) {
        if (premiumManager.isPremiumActive()) {
            _payState.value = PayState.AlreadyPurchased
            return
        }

        // 初始化小米支付
        miPayManager.init(activity)
        
        // 显示支付方式选择
        _payState.value = PayState.ShowPayOptions
    }

    /**
     * 选择支付方式
     */
    fun selectPayMethod(method: PayMethod) {
        when (method) {
            PayMethod.XIAOMI_PAY -> {
                _payState.value = PayState.Loading
                miPayManager.startPurchase()
            }
            PayMethod.EMAIL -> {
                contactForPayment()
            }
            PayMethod.TEST -> {
                simulatePurchaseSuccess()
            }
        }
    }

    /**
     * 恢复购买
     */
    fun restorePurchase() {
        _payState.value = PayState.Loading
        miPayManager.restorePurchase()
    }

    /**
     * 处理支付回调
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        miPayManager.handleActivityResult(requestCode, resultCode, data)
    }

    /**
     * 联系客服购买
     */
    fun contactForPayment() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$CONTACT_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, "医迹高级版购买")
            putExtra(Intent.EXTRA_TEXT, """
                我想购买医迹高级版（$PRODUCT_PRICE_DISPLAY）

                设备信息：
                - 应用版本：${getAppVersion()}
                - 设备型号：${android.os.Build.MODEL}
                - 系统版本：${android.os.Build.VERSION.RELEASE}

                请告诉我支付方式，谢谢！
            """.trimIndent())
        }
        
        try {
            context.startActivity(Intent.createChooser(intent, "发送邮件"))
            _payState.value = PayState.ContactEmailSent
        } catch (e: Exception) {
            _payState.value = PayState.Error("无法打开邮件客户端")
        }
    }

    /**
     * 模拟购买成功（测试用）
     */
    fun simulatePurchaseSuccess() {
        val orderId = "test_${System.currentTimeMillis()}"
        premiumManager.savePurchaseReceipt(orderId, "test_receipt")
        _payState.value = PayState.Success(orderId)
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _payState.value = PayState.Idle
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "未知"
        } catch (e: Exception) {
            "未知"
        }
    }
}

enum class PayMethod {
    XIAOMI_PAY,
    EMAIL,
    TEST
}

sealed class PayState {
    data object Idle : PayState()
    data object Loading : PayState()
    data object ShowPayOptions : PayState()
    data object AlreadyPurchased : PayState()
    data object ContactEmailSent : PayState()
    data class Success(val orderId: String) : PayState()
    data class Error(val message: String) : PayState()
}
```

### 6. 更新MainActivity（处理支付回调）

```kotlin
// 在MainActivity中添加
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    paymentManager.handleActivityResult(requestCode, resultCode, data)
}
```

---

## 三、测试

### 1. 测试模式
- 调试版本中可以使用测试按钮模拟购买
- 正式版本中测试按钮不可见

### 2. 测试账号
- 使用小米提供的测试账号
- 测试环境不真实扣费

### 3. 测试步骤
1. 清除应用数据
2. 点击购买
3. 选择测试模式
4. 验证功能解锁

---

## 四、上线准备

### 1. 小米开放平台配置
- 上传APK
- 填写应用描述
- 提交审核

### 2. 应用描述示例
```
医迹是一款个人及家庭成员的就诊记录管理工具。

主要功能：
- 记录每次就诊信息
- 管理用药记录
- 为家庭成员建立独立档案
- 设置用药、复查等提醒
- 数据本地存储，支持加密备份

本应用是记录存储工具，不提供任何医疗建议、诊断或治疗功能。
```

### 3. 关键词
```
就诊记录, 病历管理, 用药记录, 家庭档案, 提醒工具
```

---

## 五、注意事项

1. **收据安全**
   - 使用EncryptedSharedPreferences加密存储
   - 不要在日志中打印receipt

2. **错误处理**
   - 网络异常时的重试机制
   - 支付失败时的用户提示

3. **用户体验**
   - 支付过程中显示loading
   - 支付结果明确反馈

4. **恢复购买**
   - 提供"恢复购买"入口
   - 处理设备更换场景
