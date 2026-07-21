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
    object Family : Screen("family", "家人档案", { tint, size ->
        Icon(Icons.Filled.People, "家人档案", tint = tint, modifier = Modifier.size(size))
    })
    object Settings : Screen("settings", "设置", { tint, size ->
        Icon(Icons.Filled.Settings, "设置", tint = tint, modifier = Modifier.size(size))
    })
    
    companion object {
        val bottomBarScreens = listOf(Home, Family)
    }
}