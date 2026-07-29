package com.yy.medtrace.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import android.widget.Toast
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.llm.AnalysisResult
import com.yy.medtrace.data.llm.AnalysisUseCase
import com.yy.medtrace.data.llm.Metric
import com.yy.medtrace.data.llm.preferredVisitDateTime
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.data.model.MedicationItem
import com.yy.medtrace.data.settings.LlmSettingsStore
import com.yy.medtrace.ui.state.SelectedMemberHolder
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.ui.theme.dashedBorder
import com.yy.medtrace.navigation.Screen
import com.yy.medtrace.ui.theme.Primary
import com.yy.medtrace.ui.components.MemberSelector
import com.yy.medtrace.ui.components.SectionCard
import kotlinx.serialization.json.Json
import com.yy.medtrace.util.bitmapToBase64
import com.yy.medtrace.util.uriToBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private fun List<MedicationItem>.updateAt(
    index: Int,
    transform: MedicationItem.() -> MedicationItem
): List<MedicationItem> = mapIndexed { i, m -> if (i == index) m.transform() else m }

private fun encodeMetrics(metrics: List<Metric>): String =
    Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Metric.serializer()), metrics)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicalRecordScreen(
    database: AppDatabase,
    navController: NavController,
    recordId: Long = -1L,
    premiumManager: com.yy.medtrace.data.settings.PremiumManager? = null
) {
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var diagnosis by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var medItems by remember { mutableStateOf<List<MedicationItem>>(emptyList()) }
    var notes by remember { mutableStateOf("") }
    var onsetTime by remember { mutableStateOf(LocalDateTime.now()) }
    var error by remember { mutableStateOf<String?>(null) }
    var existingId by remember { mutableStateOf<Long?>(null) }
    var existingMetricsJson by remember { mutableStateOf("") }

    var noteText by remember { mutableStateOf("") }
    var images by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var analysisError by remember { mutableStateOf<String?>(null) }
    var analysisJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var analysisProgress by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val analysisUseCase = remember { AnalysisUseCase(LlmSettingsStore(context)) }

    LaunchedEffect(Unit) {
        database.familyMemberDao().getAllMembers().collect { list ->
            members = list
            if (selectedMemberId == null && list.isNotEmpty()) {
                selectedMemberId = database.familyMemberDao().getDefaultMember()?.id ?: list.first().id
            }
        }
    }

    LaunchedEffect(recordId) {
        if (recordId != -1L) {
            database.medicalRecordDao().getRecordById(recordId)?.let { r ->
                existingId = r.id
                selectedMemberId = r.patientId
                diagnosis = r.diagnosis
                hospital = r.hospital
                medItems = r.medItems
                notes = r.notes
                onsetTime = r.onsetTime
                existingMetricsJson = r.metricsJson
            }
        }
    }

    fun runAnalysis() {
        analysisError = null
        if (images.isEmpty() && noteText.isBlank()) {
            analysisError = "请先拍照 / 从相册选择图片，或粘贴文本"
            return
        }
        analysisJob?.cancel()
        analyzing = true
        analysisProgress = "正在准备数据..."
        analysisJob = scope.launch(Dispatchers.IO) {
            try {
                analysisProgress = "正在转换图片..."
                val imgs = images.map { bitmapToBase64(it) }
                analysisProgress = "正在调用 AI 识别..."
                analysisResult = analysisUseCase.analyze(noteText, imgs)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                analysisError = e.message ?: "识别失败"
            } finally {
                analyzing = false
                analysisProgress = ""
            }
        }
    }

    fun handleImage(uri: Uri) {
        scope.launch(Dispatchers.IO) {
            uriToBitmap(context, uri)?.let { bmp ->
                images = images + bmp
                runAnalysis()
            }
        }
    }

    val photoFile = remember { File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg") }
    val photoUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) handleImage(photoUri)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleImage(it) }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(photoUri)
        else Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
    }

    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) cameraLauncher.launch(photoUri)
        else cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    LaunchedEffect(analysisResult) {
        analysisResult?.let { r ->
            if (diagnosis.isBlank()) diagnosis = r.diagnosis ?: ""
            if (hospital.isBlank()) hospital = r.hospital ?: ""
            if (medItems.isEmpty() && r.medications.isNotEmpty()) {
                medItems = r.medications.map { MedicationItem(it.name, it.dose, it.freq, it.duration) }
            }
            r.preferredVisitDateTime()?.let { onsetTime = it }
            if (notes.isBlank()) {
                val extra = listOf(
                    r.followUp,
                    r.items.joinToString("；") { it.name + if (it.note.isNotBlank()) "（${it.note}）" else "" },
                    r.source?.let { "来源：$it" }
                ).filter { !it.isNullOrBlank() }.joinToString("\n")
                notes = extra
            }
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = if (existingId != null) "编辑就诊记录" else "添加就诊记录",
                subtitle = "智能识别，快速记录",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(Icons.Default.Home, "返回主页")
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
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 家庭成员选择
            SectionCard(title = "选择家庭成员") {
                MemberSelector(
                    members = members,
                    selectedMemberId = selectedMemberId,
                    onSelect = { selectedMemberId = it.id },
                    emptyHint = "暂无成员，请先在家庭页面添加"
                )
            }

            // AI 智能识别区（需要配置 AI 才能使用）
            SectionCard(title = "AI 智能识别") {
                // 检查是否已配置 AI
                val llmSettings = remember { com.yy.medtrace.data.settings.LlmSettingsStore(context) }
                var isAiConfigured by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    isAiConfigured = runCatching {
                        llmSettings.getApiKey()?.isNotBlank() == true
                    }.getOrDefault(false)
                }
                
                if (!isAiConfigured) {
                    // 未配置 AI：显示配置提示
                    Surface(
                        shape = AppShapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💡",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "需要配置 AI 才能使用此功能",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "请在设置 → AI 配置中添加 API Key",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                Text("去设置")
                            }
                        }
                    }
                } else {
                    // 已配置 AI：显示完整功能
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { launchCamera() },
                            enabled = !analyzing,
                            modifier = Modifier.weight(1f)
                        ) { Text("拍照识别") }
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            enabled = !analyzing,
                            modifier = Modifier.weight(1f)
                        ) { Text("相册选择") }
                    }

                    if (images.isNotEmpty()) {
                        Text("已添加 ${images.size} 张图片", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (analyzing) {
                        Surface(
                            shape = AppShapes.small,
                            color = Primary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Primary
                                )
                                Column {
                                    Text(
                                        "AI 识别中...",
                                        color = Primary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (analysisProgress.isNotBlank()) {
                                        Text(
                                            analysisProgress,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    analysisError?.let {
                        Text("识别失败：$it", color = MaterialTheme.colorScheme.error)
                    }

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("粘贴文本（可选，如病历文字）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )

                    if (noteText.isNotBlank()) {
                        OutlinedButton(
                            onClick = { runAnalysis() },
                            enabled = !analyzing,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("分析文本") }
                    }

                    // 免责声明
                    Surface(
                        shape = AppShapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⚠️",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "识别结果仅供参考，请以实际病历为准",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 基本信息
            SectionCard(title = "基本信息") {
                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text("就诊类型") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it },
                    label = { Text("就诊医院（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        val currentDateTime = onsetTime
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        onsetTime = LocalDateTime.of(
                                            year, month + 1, dayOfMonth,
                                            hourOfDay, minute
                                        )
                                    },
                                    currentDateTime.hour,
                                    currentDateTime.minute,
                                    true
                                ).show()
                            },
                            currentDateTime.year,
                            currentDateTime.monthValue - 1,
                            currentDateTime.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, "选择日期时间")
                    Spacer(Modifier.width(8.dp))
                    Text("就诊时间：${onsetTime.format(dateTimeFormatter)}")
                }
            }

            // 用药记录
            SectionCard(title = "用药记录") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    medItems.forEachIndexed { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppShapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("药品 ${index + 1}", style = MaterialTheme.typography.titleSmall)
                                    OutlinedButton(
                                        onClick = { medItems = medItems.filterIndexed { i, _ -> i != index } },
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, "删除", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("删除")
                                    }
                                }
                                OutlinedTextField(
                                    value = item.name,
                                    onValueChange = { medItems = medItems.updateAt(index) { copy(name = it) } },
                                    label = { Text("药品名称") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = item.dose,
                                        onValueChange = { medItems = medItems.updateAt(index) { copy(dose = it) } },
                                        label = { Text("剂量") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = item.freq,
                                        onValueChange = { medItems = medItems.updateAt(index) { copy(freq = it) } },
                                        label = { Text("频次") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = item.duration,
                                    onValueChange = { medItems = medItems.updateAt(index) { copy(duration = it) } },
                                    label = { Text("疗程（可选）") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { medItems = medItems + MedicationItem() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("添加药品")
                    }
                }
            }

            // 备注
            SectionCard(title = "备注") {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val canSave = selectedMemberId != null
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        val selectedMember = members.firstOrNull { it.id == selectedMemberId }
                        if (selectedMemberId == null || selectedMember == null) {
                            error = "请选择所属家庭成员"
                            return@Button
                        }
                        if (diagnosis.isBlank() || medItems.isEmpty()) {
                            error = "请填写就诊类型与至少一项药品"
                            return@Button
                        }

                        val dosage = medItems.firstOrNull { it.dose.isNotBlank() }?.dose ?: ""
                        val frequency = medItems.map { it.freq }.filter { it.isNotBlank() }
                            .distinct().joinToString("；")

                        val metricsJson = analysisResult?.metrics?.takeIf { it.isNotEmpty() }
                            ?.let { encodeMetrics(it) }
                            ?: existingMetricsJson

                        val record = MedicalRecord(
                            id = existingId ?: 0,
                            patientId = selectedMember.id,
                            patientName = selectedMember.name,
                            diagnosis = diagnosis,
                            onsetTime = onsetTime,
                            hospital = hospital,
                            medItems = medItems,
                            frequency = frequency,
                            dosage = dosage,
                            notes = notes,
                            metricsJson = metricsJson
                        )

                        scope.launch {
                            try {
                                if (existingId != null) {
                                    database.medicalRecordDao().update(record)
                                    android.util.Log.d("AddRecord", "updated id=${record.id}")
                                } else {
                                    val id = database.medicalRecordDao().insert(record)
                                    val count = database.medicalRecordDao().count()
                                    android.util.Log.d("AddRecord", "inserted id=$id, total=$count")
                                }
                                SelectedMemberHolder.select(selectedMember.id, database)
                                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                                navController.navigate("medical_records") {
                                    popUpTo("medical_records") { inclusive = true }
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "保存失败"
                                e.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canSave
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun FeatureTag(text: String) {
    Surface(
        shape = AppShapes.small,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
