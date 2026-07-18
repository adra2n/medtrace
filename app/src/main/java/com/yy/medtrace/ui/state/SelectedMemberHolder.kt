package com.yy.medtrace.ui.state

import androidx.compose.runtime.mutableStateOf
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.UserSettings
import kotlinx.coroutines.flow.firstOrNull

object SelectedMemberHolder {
    // 全应用唯一的「当前选中成员」来源，首页/记录/趋势/家庭/添加记录共用，避免多来源不一致。
    val selectedMemberId = mutableStateOf<Long?>(null)

    // 设置选中成员并持久化到 UserSettings，保证跨页面与重启一致。
    suspend fun select(id: Long, database: AppDatabase) {
        selectedMemberId.value = id
        database.userSettingsDao().getUserSettings().firstOrNull()?.let { s ->
            database.userSettingsDao().insertOrUpdate(s.copy(selectedMemberId = id))
        }
    }
}
