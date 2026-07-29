package com.yy.medtrace.ui.screens

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yy.medtrace.data.settings.PremiumManager
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    navController: NavController,
    premiumManager: PremiumManager
) {
    val context = LocalContext.current
    var activationCode by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    
    val isPremiumActive = remember { premiumManager.isPremiumActive() }
    val deviceId = remember { premiumManager.getDeviceId() }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "高级版",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 状态卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremiumActive) Primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (isPremiumActive) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isPremiumActive) "已激活高级版" else "未激活",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (isPremiumActive) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 功能列表
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "高级版功能",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FeatureItem("无限家庭成员")
                    FeatureItem("无限就诊记录")
                    FeatureItem("无限AI识别")
                    FeatureItem("数据导出")
                    FeatureItem("加密备份")
                    FeatureItem("云端同步")
                }
            }

            // 设备ID
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "您的设备ID",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "请联系开发者获取注册码",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = deviceId,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) 
                                as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Device ID", deviceId)
                            clipboard.setPrimaryClip(clip)
                        }) {
                            Icon(Icons.Default.ContentCopy, "复制")
                        }
                    }
                }
            }

            // 注册码输入
            if (!isPremiumActive) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "输入注册码",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = activationCode,
                            onValueChange = { activationCode = it },
                            label = { Text("注册码") },
                            placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Button(
                            onClick = {
                                if (activationCode.isBlank()) {
                                    message = "请输入注册码"
                                    isSuccess = false
                                } else {
                                    val success = premiumManager.activateVip(activationCode)
                                    if (success) {
                                        message = "✅ 激活成功！"
                                        isSuccess = true
                                    } else {
                                        message = "❌ 注册码无效，请检查后重试"
                                        isSuccess = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("激活")
                        }
                        
                        message?.let {
                            Text(
                                text = it,
                                color = if (isSuccess) Primary else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 说明
            Text(
                text = """
                    如何获取注册码：
                    1. 复制上方设备ID发送给开发者
                    2. 支付后获取注册码
                    3. 输入注册码激活高级版
                    
                    联系方式：cljkle@163.com
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureItem(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Primary
        )
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}
