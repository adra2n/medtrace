package com.yy.medtrace.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yy.medtrace.payment.PaymentManager
import com.yy.medtrace.payment.PayMethod
import com.yy.medtrace.payment.PayState
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    navController: NavController,
    paymentManager: PaymentManager,
    onPurchaseSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val payState by paymentManager.payState.collectAsState()

    LaunchedEffect(payState) {
        when (payState) {
            is PayState.Success -> {
                onPurchaseSuccess()
                navController.popBackStack()
            }
            is PayState.Error -> {
                kotlinx.coroutines.delay(2000)
                paymentManager.resetState()
            }
            is PayState.AlreadyPurchased -> {
                navController.popBackStack()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "解锁全部功能",
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
            // 价格展示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Primary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "医迹高级版",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        PaymentManager.PRODUCT_PRICE_DISPLAY,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "一次购买，永久使用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            // 功能对比
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "功能对比",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 免费版
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("免费版", style = MaterialTheme.typography.bodyLarge)
                        Text("¥0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        FeatureItem("就诊记录", "最多 10 条", false)
                        FeatureItem("家庭成员", "1 人", false)
                        FeatureItem("AI 识别", "3 次/天", false)
                        FeatureItem("数据导出", "不支持", false)
                        FeatureItem("加密备份", "不支持", false)
                    }

                    HorizontalDivider()

                    // 高级版
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("高级版", style = MaterialTheme.typography.bodyLarge, color = Primary)
                        Text(PaymentManager.PRODUCT_PRICE_DISPLAY, style = MaterialTheme.typography.bodyMedium, color = Primary, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        FeatureItem("就诊记录", "无限制", true)
                        FeatureItem("家庭成员", "无限制", true)
                        FeatureItem("AI 识别", "无限制", true)
                        FeatureItem("数据导出", "支持", true)
                        FeatureItem("数据导入", "支持", true)
                        FeatureItem("加密备份", "支持", true)
                        FeatureItem("Gist 云端同步", "支持", true)
                    }
                }
            }

            // 支付方式选择
            when (payState) {
                is PayState.ShowPayOptions -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "选择支付方式",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // 小米支付（暂未集成）
                            OutlinedButton(
                                onClick = { paymentManager.selectPayMethod(PayMethod.XIAOMI_PAY) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("小米支付（开发中）")
                            }
                            
                            // 邮件购买
                            Button(
                                onClick = { paymentManager.selectPayMethod(PayMethod.EMAIL) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("邮件购买 ${PaymentManager.PRODUCT_PRICE_DISPLAY}")
                            }
                            
                            // 测试按钮
                            if (com.yy.medtrace.BuildConfig.DEBUG) {
                                OutlinedButton(
                                    onClick = { paymentManager.selectPayMethod(PayMethod.TEST) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("测试：模拟购买成功")
                                }
                            }
                        }
                    }
                }
                is PayState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            (payState as PayState.Error).message,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                is PayState.AlreadyPurchased -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            "您已购买高级版！",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                else -> {}
            }

            // 购买按钮（未显示支付选项时）
            if (payState !is PayState.ShowPayOptions && payState !is PayState.AlreadyPurchased) {
                Button(
                    onClick = { activity?.let { paymentManager.startPurchase(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = payState !is PayState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (payState is PayState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("立即购买 ${PaymentManager.PRODUCT_PRICE_DISPLAY}", fontSize = 16.sp)
                    }
                }
            }

            // 说明
            Text(
                text = """
                    购买须知：
                    1. 购买后立即生效，永久使用
                    2. 支持同一账号在多台设备上使用
                    3. 如有问题请联系：${PaymentManager.CONTACT_EMAIL}
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureItem(
    name: String,
    value: String,
    included: Boolean
) {
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
            tint = if (included) Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
