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
 * 用于管理应用内购买流程
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
     * 注意：当前为测试模式，实际使用时需要集成小米支付SDK
     */
    fun startPurchase(activity: Activity) {
        if (premiumManager.isPremiumActive()) {
            _payState.value = PayState.AlreadyPurchased
            return
        }

        _payState.value = PayState.Loading
        
        // 测试模式：直接模拟购买成功
        // 实际项目中，这里应该调用小米支付SDK
        _payState.value = PayState.NeedContactForPayment
    }

    /**
     * 联系客服购买
     * 用户通过邮件联系开发者进行购买
     */
    fun contactForPayment() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$CONTACT_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, "医迹高级版购买")
            putExtra(Intent.EXTRA_TEXT, """
                我想购买医迹高级版（¥9.90）

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
     * 验证购买状态
     */
    fun verifyPurchase(): Boolean {
        return premiumManager.verifyPurchase()
    }

    /**
     * 模拟购买成功（测试用）
     */
    fun simulatePurchaseSuccess(orderId: String = "test_${System.currentTimeMillis()}") {
        premiumManager.savePurchase(orderId)
        _payState.value = PayState.Success(orderId)
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
 * 支付状态
 */
sealed class PayState {
    data object Idle : PayState()
    data object Loading : PayState()
    data object AlreadyPurchased : PayState()
    data object NeedContactForPayment : PayState()
    data object ContactEmailSent : PayState()
    data class Success(val orderId: String) : PayState()
    data class Error(val message: String) : PayState()
}
