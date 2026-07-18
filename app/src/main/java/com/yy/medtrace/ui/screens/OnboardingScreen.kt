package com.yy.medtrace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yy.medtrace.data.settings.OnboardingStore
import com.yy.medtrace.ui.theme.PrimaryGradient
import kotlinx.coroutines.launch

private data class OnboardPage(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val desc: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { OnboardingStore(context) }
    var page by remember { mutableStateOf(0) }

    val pages = listOf(
        OnboardPage(Icons.Filled.AutoAwesome, "AI 智能识别",
            "拍照或粘贴处方文本，自动提取诊断、用药与检查指标，省去手动录入。"),
        OnboardPage(Icons.Filled.People, "家庭健康管理",
            "为每位家人建立健康档案，归类历次就诊记录与注意事项。"),
        OnboardPage(Icons.Filled.ShowChart, "趋势与解读",
            "检查指标自动成图，AI 给出趋势解读与健康建议，异常一目了然。")
    )

    fun finish() {
        scope.launch {
            store.setDone()
            navController.navigate("home") {
                popUpTo("onboarding") { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(PrimaryGradient)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { finish() }) { Text("跳过", color = Color.White) }
            }

            Spacer(Modifier.weight(1f))

            val current = pages[page]
            Icon(imageVector = current.icon, contentDescription = null,
                tint = Color.White, modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(24.dp))
            Text(text = current.title, color = Color.White, fontSize = 26.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(text = current.desc, color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp))

            Spacer(Modifier.weight(1f))

            Row(modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { i ->
                    Box(modifier = Modifier.size(if (i == page) 24.dp else 8.dp, 8.dp)
                        .background(if (i == page) Color.White else Color.White.copy(alpha = 0.4f), CircleShape))
                }
            }

            Button(
                onClick = { if (page < pages.lastIndex) page++ else finish() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(if (page < pages.lastIndex) "下一步" else "开始使用")
            }
        }
    }
}
