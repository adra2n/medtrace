package com.yy.chiyaole.ui.state

import androidx.compose.runtime.mutableStateOf
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.UserSettings
import kotlinx.coroutines.flow.firstOrNull

// 已删除成员留下的记录会被归入 patientId=0（未归属），用一个虚拟成员在选人器中展示。
val UNKNOWN_MEMBER = FamilyMember(id = 0, name = "未归属", relation = "历史记录")

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
