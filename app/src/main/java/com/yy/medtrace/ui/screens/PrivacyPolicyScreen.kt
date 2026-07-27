package com.yy.medtrace.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yy.medtrace.ui.theme.GradientTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            GradientTopBar(
                title = "隐私政策",
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // 最后更新日期
            Text(
                text = "最后更新日期：2026年7月25日",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 一、开发者信息
            SectionTitle("一、开发者信息")
            ContentText("应用名称：医迹")
            ContentText("开发者：天津市津南区亦阳智创软件开发工作室")
            ContentText("联系方式：cljkle@163.com")

            // 二、数据收集
            SectionTitle("二、数据收集")
            ContentText("本应用收集以下数据：")
            ContentText("1. 匿名使用统计（通过友盟 SDK）")
            ContentText("   - 启动次数")
            ContentText("   - 页面访问")
            ContentText("   - 设备型号（用于适配）")
            ContentText("   - 崩溃日志")
            ContentText("2. 用户主动输入的数据")
            ContentText("   - 健康记录")
            ContentText("   - 用药信息")
            ContentText("   - 家庭成员信息")

            // 三、数据存储
            SectionTitle("三、数据存储")
            ContentText("所有健康数据仅存储在用户设备本地。")
            ContentText("支持本地备份（JSON 格式，可选加密）。")
            ContentText("不会上传至任何服务器。")

            // 四、第三方服务
            SectionTitle("四、第三方服务")
            ContentText("本应用使用友盟 SDK 进行统计分析。")

            // 五、用户权利
            SectionTitle("五、用户权利")
            ContentText("您有权：")
            ContentText("- 查看所有收集的数据")
            ContentText("- 删除所有数据")
            ContentText("- 导出所有数据")
            ContentText("- 撤回隐私同意")

            // 六、未成年人保护
            SectionTitle("六、未成年人保护")
            ContentText("本应用不面向 16 周岁以下未成年人提供服务。")

            // 七、隐私政策更新
            SectionTitle("七、隐私政策更新")
            ContentText("我们可能会不时更新本隐私政策，更新后会在应用内通知。")

            // 八、争议解决
            SectionTitle("八、争议解决")
            ContentText("本协议适用中华人民共和国法律。")
            ContentText("争议解决地为天津市津南区。")

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ContentText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 22.sp
    )
}
