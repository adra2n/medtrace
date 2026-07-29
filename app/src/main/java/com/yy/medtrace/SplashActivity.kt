package com.yy.medtrace

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.yy.medtrace.common.Constants
import com.yy.medtrace.data.settings.OnboardingStore
import com.yy.medtrace.data.settings.PrivacyConsentStore
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private var splashContainer: FrameLayout? = null
    private var fallbackLayout: LinearLayout? = null
    private var skipTextView: TextView? = null
    private var countDownTimer: CountDownTimer? = null
    private var navigated = false

    companion object {
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

        lifecycleScope.launch {
            val privacyGranted = runCatching {
                PrivacyConsentStore(this@SplashActivity).isGranted()
            }.getOrDefault(false)

            if (privacyGranted) {
                initAnalytics()
                showSplash()
            } else {
                navigateToNext()
            }
        }
    }

    private fun initAnalytics() {
        UMConfigure.preInit(this, Constants.UMENG_APPKEY, Constants.UMENG_CHANNEL)
        UMConfigure.submitPolicyGrantResult(this, true)
        UMConfigure.init(this, Constants.UMENG_APPKEY, Constants.UMENG_CHANNEL, UMConfigure.DEVICE_TYPE_PHONE, null)
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)
    }

    private fun showSplash() {
        fallbackLayout?.visibility = View.VISIBLE
        splashContainer?.visibility = View.GONE
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
        if (navigated) return
        navigated = true
        countDownTimer?.cancel()
        lifecycleScope.launch {
            val privacyGranted = runCatching {
                PrivacyConsentStore(this@SplashActivity).isGranted()
            }.getOrDefault(false)

            val nextIntent = if (privacyGranted) {
                val onboardingDone = runCatching {
                    OnboardingStore(this@SplashActivity).isDone()
                }.getOrDefault(false)
                if (onboardingDone) {
                    Intent(this@SplashActivity, MainActivity::class.java)
                } else {
                    Intent(this@SplashActivity, MainActivity::class.java).apply {
                        putExtra("navigate_to", "onboarding")
                    }
                }
            } else {
                Intent(this@SplashActivity, MainActivity::class.java).apply {
                    putExtra("navigate_to", "privacy_consent")
                }
            }

            startActivity(nextIntent)
            finish()
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
