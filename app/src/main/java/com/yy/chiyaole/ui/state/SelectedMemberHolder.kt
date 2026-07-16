package com.yy.chiyaole.ui.state

import androidx.compose.runtime.mutableStateOf

object SelectedMemberHolder {
    val homeSelectedMemberId = mutableStateOf<Long?>(null)
    val recordsSelectedMemberId = mutableStateOf<Long?>(null)
}
