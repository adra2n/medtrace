package com.yy.medtrace.ui.state

import androidx.compose.runtime.mutableStateOf
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

// 已删除成员留下的记录会被归入 patientId=0（未归属），用一个虚拟成员在选人器中展示。
val UNKNOWN_MEMBER = FamilyMember(id = 0, name = "未归属", relation = "历史记录")

object SelectedMemberHolder {
    // Compose 可观察状态，Screen 通过 .value 读取
    val selectedMemberId = mutableStateOf<Long?>(null)

    // 设置选中成员：同步更新 Compose 状态 + 持久化到 UserSettings
    suspend fun select(id: Long, database: AppDatabase) {
        selectedMemberId.value = id
        withContext(Dispatchers.IO) {
            database.userSettingsDao().getUserSettings().firstOrNull()?.let { s ->
                database.userSettingsDao().insertOrUpdate(s.copy(selectedMemberId = id))
            }
        }
    }

    // 应用启动时从数据库加载上次选中的成员（在 MainScreen 内调用一次）
    suspend fun initFromDatabase(database: AppDatabase) {
        if (selectedMemberId.value != null) return
        withContext(Dispatchers.IO) {
            val saved = database.userSettingsDao().getUserSettings().firstOrNull()?.selectedMemberId
            if (saved != null && saved != 0L) {
                selectedMemberId.value = saved
            }
        }
    }
}
