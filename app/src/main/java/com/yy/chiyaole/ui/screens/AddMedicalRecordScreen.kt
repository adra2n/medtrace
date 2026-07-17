package com.yy.chiyaole.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
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
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.llm.AnalysisResult
import com.yy.chiyaole.data.llm.AnalysisUseCase
import com.yy.chiyaole.data.llm.Metric
import com.yy.chiyaole.data.llm.preferredVisitDateTime
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.data.model.MedicationItem
import com.yy.chiyaole.data.settings.LlmSettingsStore
import com.yy.chiyaole.ui.state.SelectedMemberHolder
import kotlinx.serialization.json.Json
import com.yy.chiyaole.ui.theme.AppShapes
import com.yy.chiyaole.util.bitmapToBase64
import com.yy.chiyaole.util.uriToBitmap
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
    recordId: Long = -1L
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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val analysisUseCase = remember { AnalysisUseCase(LlmSettingsStore(context)) }

    LaunchedEffect(Unit) {
        database.familyMemberDao().getAllMembers().collect { list ->
            members = list
            if (selectedMemberId == null && list.isNotEmpty()) {
                selectedMemberId = list.first().id
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
        analyzing = true
        scope.launch(Dispatchers.IO) {
            try {
                val imgs = images.map { bitmapToBase64(it) }
                analysisResult = analysisUseCase.analyze(noteText, imgs)
            } catch (e: Exception) {
                analysisError = e.message ?: "识别失败"
            } finally {
                analyzing = false
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
            TopAppBar(
                title = { Text(if (existingId != null) "编辑医疗记录" else "添加医疗记录") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
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
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { launchCamera() }) { Text("拍照识别") }
                Button(onClick = { galleryLauncher.launch("image/*") }) { Text("从相册选择") }
            }
            if (images.isNotEmpty()) {
                Text("待识别图片：${images.size} 张", style = MaterialTheme.typography.bodyMedium)
            }
            if (analyzing) Text("AI 识别中…", color = MaterialTheme.colorScheme.primary)
            analysisError?.let {
                Text("识别失败：$it", color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("粘贴文本（可一并分析，如处方文字）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )

            if (noteText.isNotBlank()) {
                OutlinedButton(
                    onClick = { runAnalysis() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("分析文本") }
            }

            Text("所属家庭成员", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                members.forEach { member ->
                    FilterChip(
                        selected = member.id == selectedMemberId,
                        onClick = { selectedMemberId = member.id },
                        label = { Text(member.name) }
                    )
                }
            }

            OutlinedTextField(
                value = diagnosis,
                onValueChange = { diagnosis = it },
                label = { Text("诊断结果") },
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

            Text("开具药品", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
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
                            IconButton(onClick = { medItems = medItems.filterIndexed { i, _ -> i != index } }) {
                                Icon(Icons.Default.Delete, "删除该药品")
                            }
                        }
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { medItems = medItems.updateAt(index) { copy(name = it) } },
                            label = { Text("名称") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = item.dose,
                            onValueChange = { medItems = medItems.updateAt(index) { copy(dose = it) } },
                            label = { Text("剂量（如 0.5g）") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = item.freq,
                            onValueChange = { medItems = medItems.updateAt(index) { copy(freq = it) } },
                            label = { Text("频次（如 每日3次）") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = item.duration,
                            onValueChange = { medItems = medItems.updateAt(index) { copy(duration = it) } },
                            label = { Text("疗程（如 7天）") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = { medItems = medItems + MedicationItem() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("添加药品")
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val selectedMember = members.firstOrNull { it.id == selectedMemberId }
                        if (selectedMemberId == null || selectedMember == null) {
                            error = "请选择所属家庭成员"
                            return@Button
                        }
                        if (diagnosis.isBlank() || medItems.isEmpty()) {
                            error = "请填写诊断结果与至少一项药品"
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
                                SelectedMemberHolder.recordsSelectedMemberId.value = selectedMember.id
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
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
            }
        }
    }
}
