package com.yy.medtrace.data.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用户界面模式
 */
enum class UserMode {
    STANDARD,  // 标准版
    ELDERLY    // 长辈版
}

/**
 * 界面模式存储管理
 * 支持标准版和长辈版切换
 */
@Singleton
class UserModeStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _currentMode = MutableStateFlow(loadMode())
    val currentMode: StateFlow<UserMode> = _currentMode.asStateFlow()

    /**
     * 获取当前模式
     */
    fun getMode(): UserMode {
        return _currentMode.value
    }

    /**
     * 设置模式
     */
    fun setMode(mode: UserMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _currentMode.value = mode
    }

    /**
     * 是否为长辈版
     */
    fun isElderlyMode(): Boolean {
        return _currentMode.value == UserMode.ELDERLY
    }

    /**
     * 从存储中加载模式
     */
    private fun loadMode(): UserMode {
        val modeName = prefs.getString(KEY_MODE, UserMode.STANDARD.name)
        return try {
            UserMode.valueOf(modeName ?: UserMode.STANDARD.name)
        } catch (e: Exception) {
            UserMode.STANDARD
        }
    }

    companion object {
        private const val PREFS_NAME = "user_mode_prefs"
        private const val KEY_MODE = "current_mode"
    }
}
