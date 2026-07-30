package com.yy.medtrace.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                text = "最后更新日期：2026年7月27日",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 重要声明
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "重要声明：本应用是记录存储工具，不是医疗器械，不提供任何医疗建议、诊断或治疗功能。如有健康问题，请咨询专业医生。",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            // 一、服务条款
            SectionTitle("一、服务条款")
            ContentText("1.1 医迹是一款个人及家庭成员的就诊记录存储与管理工具。")
            ContentText("1.2 本应用仅提供数据记录、存储和归档功能。")
            ContentText("1.3 本应用不提供任何医疗建议、诊断或治疗功能。")
            ContentText("1.4 用户应自行承担使用本应用产生的风险。")

            // 二、功能说明
            SectionTitle("二、功能说明")
            ContentText("本应用提供的功能包括：")
            ContentText("- 就诊记录的添加、编辑和删除")
            ContentText("- 用药记录的管理和提醒")
            ContentText("- 家庭成员档案管理")
            ContentText("- 数据本地存储和备份")
            ContentText("- 智能识别（仅用于提取文字信息，不做任何判断）")

            // 三、免责声明
            SectionTitle("三、免责声明")
            ContentText("3.1 本应用提供的所有信息仅供参考，不能替代专业医疗建议。")
            ContentText("3.2 本应用不是医疗器械，不提供诊断、治疗或健康评估功能。")
            ContentText("3.3 智能识别功能仅用于提取文字信息，识别结果可能存在误差。")
            ContentText("3.4 用户应咨询专业医生获取医疗建议。")
            ContentText("3.5 开发者不对因使用本应用产生的任何损失承担责任。")

            // 四、数据安全
            SectionTitle("四、数据安全")
            ContentText("4.1 用户数据仅存储在本地设备，不会上传至任何服务器。")
            ContentText("4.2 用户可随时导出或删除自己的数据。")
            ContentText("4.3 开发者无法访问用户的数据。")

            // 五、知识产权
            SectionTitle("五、知识产权")
            ContentText("5.1 本应用的知识产权归开发者所有。")
            ContentText("5.2 用户生成的数据归用户所有。")

            // 六、协议更新
            SectionTitle("六、协议更新")
            ContentText("我们保留随时修改本协议的权利。")
            ContentText("继续使用本应用即表示您同意修改后的协议。")

            // 七、争议解决
            SectionTitle("七、争议解决")
            ContentText("本协议适用中华人民共和国法律。")
            ContentText("争议解决地为天津市津南区。")

            // 八、联系方式
            SectionTitle("八、联系方式")
            ContentText("如有疑问，请联系：cljkle@163.com")

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
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
