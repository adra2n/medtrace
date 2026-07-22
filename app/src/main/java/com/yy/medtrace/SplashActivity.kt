package com.yy.medtrace

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yy.medtrace.data.settings.OnboardingStore
import com.yy.medtrace.data.settings.PrivacyConsentStore
import kotlinx.coroutines.runBlocking

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private var splashContainer: FrameLayout? = null
    private var skipTextView: TextView? = null
    private var countDownTimer: CountDownTimer? = null

    companion object {
        private const val SKIP_DELAY = 5000L
        private const val COUNT_DOWN_INTERVAL = 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        splashContainer = findViewById(R.id.splash_container)
        skipTextView = findViewById(R.id.skip_text)

        skipTextView?.setOnClickListener {
            navigateToNext()
        }

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
