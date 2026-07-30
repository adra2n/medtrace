package com.yy.medtrace.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.medtrace.MedTraceApplication
import com.yy.medtrace.data.settings.OnboardingStore
import com.yy.medtrace.data.settings.PrivacyConsentStore
import com.yy.medtrace.ui.theme.Primary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyConsentScreen(
    navController: NavController,
    onDecline: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { PrivacyConsentStore(context) }

    fun proceed() {
        scope.launch {
            store.setGranted()
            MedTraceApplication.initAnalytics(context)
            val next = if (OnboardingStore(context).isDone()) "home" else "onboarding"
            navController.navigate(next) {
                popUpTo("privacy_consent") { inclusive = true }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "隐私政策与用户协议",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "欢迎使用医迹。我们非常重视您的个人信息和隐私保护。在继续使用前，请阅读并同意以下说明：",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "· 我们仅收集用于改进产品体验的匿名统计数据（如启动次数、页面访问），不包含您的任何健康记录。\n" +
                        "· 统计数据由第三方分析服务（友盟）处理，您可随时在系统设置中清除应用数据以停止收集。\n" +
                        "· 您的健康档案仅存储于本机设备，不会上传至任何服务器。",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "友盟隐私政策",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.umeng.com/page/policy")))
                    }
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "点击「同意」，即表示您理解并同意上述隐私处理方式。",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { proceed() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("同意并继续", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onDecline() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("不同意", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "若选择「不同意」，应用将无法启动。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
