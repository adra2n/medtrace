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
        // 启动页最短展示时间。原先固定 3s，是冷启动链路中最长的一段无谓等待。
        private const val SKIP_DELAY = 1200L
        private const val COUNT_DOWN_INTERVAL = 400L
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

    /**
     * 复用 [MedTraceApplication.initAnalytics]：UMConfigure.init 在 IO 线程执行，
     * 不再阻塞启动页主线程（原先这里同步调用了三次友盟 API）。
     */
    private fun initAnalytics() {
        MedTraceApplication.initAnalytics(this)
    }

    private fun showSplash() {
        fallbackLayout?.visibility = View.VISIBLE
        splashContainer?.visibility = View.GONE
        startCountDown()
    }

    private fun startCountDown() {
        countDownTimer?.cancel()
        skipTextView?.text = getString(R.string.splash_skip_countdown, (SKIP_DELAY / 1000).coerceAtLeast(1))
        countDownTimer = object : CountDownTimer(SKIP_DELAY, COUNT_DOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).coerceAtLeast(1)
                skipTextView?.text = getString(R.string.splash_skip_countdown, secondsLeft)
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
