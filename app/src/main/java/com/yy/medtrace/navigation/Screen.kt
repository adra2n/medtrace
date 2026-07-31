package com.yy.medtrace.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed class Screen(
    val route: String,
    val label: String,
    val icon: @Composable (tint: Color, size: Dp) -> Unit
) {
    object Home : Screen("home", "首页", { tint, size ->
        Icon(Icons.Filled.Home, "首页", tint = tint, modifier = Modifier.size(size))
    })
    object MedicalRecords : Screen("medical_records", "记录", { tint, size ->
        Icon(Icons.Filled.FolderOpen, "记录", tint = tint, modifier = Modifier.size(size))
    })
    object Reminders : Screen("reminders", "提醒", { tint, size ->
        Icon(Icons.Filled.AlarmOn, "提醒", tint = tint, modifier = Modifier.size(size))
    })
    object Profile : Screen("profile", "我的", { tint, size ->
        Icon(Icons.Filled.Person, "我的", tint = tint, modifier = Modifier.size(size))
    })
    object Settings : Screen("settings", "设置", { tint, size ->
        Icon(Icons.Filled.Settings, "设置", tint = tint, modifier = Modifier.size(size))
    })
    
    companion object {
        val bottomBarScreens = listOf(Home, MedicalRecords, Reminders, Profile)
    }
}