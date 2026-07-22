package com.yy.medtrace

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.umeng.ads.utils.UMAdCallback
import com.umeng.ads.utils.UMAdSplashHelper
import com.yy.medtrace.data.settings.OnboardingStore
import com.yy.medtrace.data.settings.PrivacyConsentStore
import kotlinx.coroutines.runBlocking

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private var splashContainer: FrameLayout? = null
    private var skipTextView: TextView? = null
    private var countDownTimer: CountDownTimer? = null
    private var isAdLoaded = false

    companion object {
        private const val SPLASH_AD_TIMEOUT = 3500L
        private const val COUNT_DOWN_INTERVAL = 1000L
        private const val SKIP_DELAY = 5000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        splashContainer = findViewById(R.id.splash_container)
        skipTextView = findViewById(R.id.skip_text)

        skipTextView?.setOnClickListener {
            navigateToNext()
        }

        loadSplashAd()
    }

    private fun loadSplashAd() {
        val appKey = "6a5b46a5cbfa6959517c8588"
        val adUnitId = "100012523"

        UMAdSplashHelper.loadSplashAd(
            this,
            appKey,
            adUnitId,
            splashContainer,
            object : UMAdCallback {
                override fun onAdLoaded() {
                    isAdLoaded = true
                    startCountDown()
                }

                override fun onAdLoadFailed(error: String?) {
                    navigateToNext()
                }

                override fun onAdShow() {
                    startCountDown()
                }

                override fun onAdClick() {
                    // 广告点击
                }

                override fun onAdDismiss() {
                    navigateToNext()
                }
            },
            SPLASH_AD_TIMEOUT
        )

        // 如果广告加载超时，直接跳转
        object : CountDownTimer(SPLASH_AD_TIMEOUT, COUNT_DOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                // 等待广告加载
            }

            override fun onFinish() {
                if (!isAdLoaded) {
                    navigateToNext()
                }
            }
        }.start()
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
        val privacyGranted = runCatching {
            PrivacyConsentStore(this).isGranted()
        }.getOrDefault(false)

        val nextIntent = if (privacyGranted) {
            val onboardingDone = runCatching {
                OnboardingStore(this).isDone()
            }.getOrDefault(false)
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
