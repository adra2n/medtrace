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
fun UserAgreementScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            GradientTopBar(
                title = "用户协议",
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

            // 一、服务条款
            SectionTitle("一、服务条款")
            ContentText("1.1 医迹是一款个人及家庭成员的医学检查记录与用药管理工具。")
            ContentText("1.2 本应用仅供参考，不构成医疗建议。")
            ContentText("1.3 用户应自行承担使用本应用产生的风险。")

            // 二、免责声明
            SectionTitle("二、免责声明")
            ContentText("2.1 本应用提供的所有信息仅供参考，不能替代专业医疗建议。")
            ContentText("2.2 AI 分析功能的结果仅供参考，不应作为医疗决策的依据。")
            ContentText("2.3 用户应咨询专业医生获取医疗建议。")
            ContentText("2.4 开发者不对因使用本应用产生的任何损失承担责任。")

            // 三、知识产权
            SectionTitle("三、知识产权")
            ContentText("3.1 本应用的知识产权归开发者所有。")
            ContentText("3.2 用户生成的数据归用户所有。")
            ContentText("3.3 未经许可，不得复制、修改、传播本应用的任何内容。")

            // 四、用户行为
            SectionTitle("四、用户行为")
            ContentText("用户同意不：")
            ContentText("- 使用本应用进行任何非法活动")
            ContentText("- 干扰本应用的正常运行")
            ContentText("- 尝试获取未授权的访问权限")

            // 五、协议更新
            SectionTitle("五、协议更新")
            ContentText("我们保留随时修改本协议的权利。")
            ContentText("继续使用本应用即表示您同意修改后的协议。")

            // 六、争议解决
            SectionTitle("六、争议解决")
            ContentText("本协议适用中华人民共和国法律。")
            ContentText("争议解决地为天津市津南区。")

            // 七、免责声明
            SectionTitle("七、重要声明")
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "本应用提供的所有健康信息仅供参考，不能替代专业医疗建议、诊断或治疗。如有健康问题，请咨询专业医生。",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

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
