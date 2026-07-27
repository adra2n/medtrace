package com.yy.medtrace

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.yy.medtrace.common.Constants
import com.yy.medtrace.data.settings.OnboardingStore
import com.yy.medtrace.data.settings.PrivacyConsentStore
import kotlinx.coroutines.runBlocking

@SuppressLint("CustomSplashScreen")
class SplashActivity : Activity() {

    private var splashContainer: FrameLayout? = null
    private var fallbackLayout: LinearLayout? = null
    private var skipTextView: TextView? = null
    private var countDownTimer: CountDownTimer? = null

    companion object {
        private const val TAG = "SplashActivity"
        private const val SKIP_DELAY = 3000L
        private const val COUNT_DOWN_INTERVAL = 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        splashContainer = findViewById(R.id.splash_container)
        fallbackLayout = findViewById(R.id.fallback_layout)
        skipTextView = findViewById(R.id.skip_text)

        skipTextView?.setOnClickListener {
            navigateToNext()
        }

        // 检查隐私协议状态
        val privacyGranted = runBlocking {
            runCatching {
                PrivacyConsentStore(this@SplashActivity).isGranted()
            }.getOrDefault(false)
        }

        Log.d(TAG, "privacyGranted: $privacyGranted")

        if (privacyGranted) {
            // 已同意隐私协议，初始化统计 SDK 并显示启动页
            initAnalytics()
            showSplash()
        } else {
            // 未同意隐私协议，直接跳转
            Log.d(TAG, "隐私协议未同意，跳转到隐私协议页面")
            navigateToNext()
        }
    }

    private fun initAnalytics() {
        UMConfigure.preInit(this, Constants.UMENG_APPKEY, Constants.UMENG_CHANNEL)
        UMConfigure.submitPolicyGrantResult(this, true)
        UMConfigure.init(this, Constants.UMENG_APPKEY, Constants.UMENG_CHANNEL, UMConfigure.DEVICE_TYPE_PHONE, null)
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)
    }

    private fun showSplash() {
        // 显示兜底布局（App图标）
        fallbackLayout?.visibility = View.VISIBLE
        splashContainer?.visibility = View.GONE

        // 启动倒计时
        startCountDown()
    }

    private fun startCountDown() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(SKIP_DELAY, COUNT_DOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                skipTextView?.text = "跳过 ${secondsLeft}s"
            }

            override fun onFinish() {
                navigateToNext()
            }
        }.start()
        skipTextView?.visibility = View.VISIBLE
    }

    private fun navigateToNext() {
        countDownTimer?.cancel()
        val privacyGranted = runBlocking {
            runCatching {
                PrivacyConsentStore(this@SplashActivity).isGranted()
            }.getOrDefault(false)
        }

        val nextIntent = if (privacyGranted) {
            val onboardingDone = runBlocking {
                runCatching {
                    OnboardingStore(this@SplashActivity).isDone()
                }.getOrDefault(false)
            }
            if (onboardingDone) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, MainActivity::class.java).apply {
                    putExtra("navigate_to", "onboarding")
                }
            }
        } else {
            Intent(this, MainActivity::class.java).apply {
                putExtra("navigate_to", "privacy_consent")
            }
        }

        startActivity(nextIntent)
        finish()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
