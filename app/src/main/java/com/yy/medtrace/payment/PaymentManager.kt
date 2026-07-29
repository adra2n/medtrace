package com.yy.medtrace.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.yy.medtrace.data.settings.PremiumManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 支付管理器
 * 支持小米支付SDK和测试模式
 */
@Singleton
class PaymentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val premiumManager: PremiumManager
) {
    companion object {
        private const val TAG = "PaymentManager"
        
        // 产品信息
        const val PRODUCT_UNLOCK_ALL = "medtrace_unlock_all"
        const val PRODUCT_NAME = "医迹高级版"
        const val PRODUCT_DESCRIPTION = "解锁所有高级功能"
        const val PRODUCT_PRICE = 990 // 单位：分
        const val PRODUCT_PRICE_DISPLAY = "¥9.90"
        
        // 联系方式
        const val CONTACT_EMAIL = "cljkle@163.com"
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

        _payState.value = PayState.Loading
        
        // 显示支付选择界面
        _payState.value = PayState.ShowPayOptions
    }

    /**
     * 选择支付方式
     */
    fun selectPayMethod(method: PayMethod) {
        when (method) {
            PayMethod.XIAOMI_PAY -> {
                // 调用小米支付SDK
                // TODO: 集成小米支付SDK
                _payState.value = PayState.Error("小米支付暂未集成，请选择其他方式")
            }
            PayMethod.EMAIL -> {
                contactForPayment()
            }
            PayMethod.TEST -> {
                // 测试模式：直接模拟购买成功
                simulatePurchaseSuccess()
            }
        }
    }

    /**
     * 联系客服购买
     */
    fun contactForPayment() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$CONTACT_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, "医迹高级版购买")
            putExtra(Intent.EXTRA_TEXT, """
                我想购买医迹高级版（${PRODUCT_PRICE_DISPLAY}）

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
            Log.e(TAG, "无法打开邮件客户端", e)
            _payState.value = PayState.Error("无法打开邮件客户端，请手动发送邮件至 $CONTACT_EMAIL")
        }
    }

    /**
     * 模拟购买成功（测试用）
     */
    fun simulatePurchaseSuccess(orderId: String = "test_${System.currentTimeMillis()}") {
        premiumManager.savePurchase(orderId)
        _payState.value = PayState.Success(orderId)
    }

    /**
     * 支付成功回调
     * 在小米支付SDK回调中调用此方法
     */
    fun onPaymentSuccess(orderId: String, receipt: String) {
        Log.d(TAG, "支付成功: orderId=$orderId")
        premiumManager.savePurchase(orderId)
        _payState.value = PayState.Success(orderId)
    }

    /**
     * 支付失败回调
     */
    fun onPaymentFailed(code: Int, message: String) {
        Log.e(TAG, "支付失败: code=$code, msg=$message")
        _payState.value = PayState.Error("支付失败: $message")
    }

    /**
     * 支付取消回调
     */
    fun onPaymentCanceled() {
        Log.d(TAG, "支付取消")
        _payState.value = PayState.Canceled
    }

    /**
     * 验证购买状态
     */
    fun verifyPurchase(): Boolean {
        return premiumManager.verifyPurchase()
    }

    /**
     * 恢复购买
     * 检查本地是否已有购买记录
     */
    fun restorePurchase() {
        _payState.value = PayState.Loading
        
        if (premiumManager.isPremiumActive()) {
            val orderId = premiumManager.getOrderId() ?: "restored"
            _payState.value = PayState.Success(orderId)
        } else {
            _payState.value = PayState.Error("未找到购买记录。如已购买请联系 $CONTACT_EMAIL")
        }
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _payState.value = PayState.Idle
    }

    /**
     * 获取应用版本
     */
    private fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "未知"
        } catch (e: Exception) {
            "未知"
        }
    }
}

/**
 * 支付方式
 */
enum class PayMethod {
    XIAOMI_PAY, // 小米支付
    EMAIL,      // 邮件购买
    TEST        // 测试模式
}

/**
 * 支付状态
 */
sealed class PayState {
    data object Idle : PayState()
    data object Loading : PayState()
    data object ShowPayOptions : PayState()
    data object AlreadyPurchased : PayState()
    data object Canceled : PayState()
    data object ContactEmailSent : PayState()
    data class Success(val orderId: String) : PayState()
    data class Error(val message: String) : PayState()
}
