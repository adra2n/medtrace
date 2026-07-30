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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import android.widget.Toast
import com.yy.medtrace.R
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
import com.yy.medtrace.ui.components.MemberSelector
import com.yy.medtrace.ui.components.SectionCard
import com.yy.medtrace.viewmodel.AddMedicalRecordViewModel
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
    viewModel: AddMedicalRecordViewModel = hiltViewModel(),
    navController: NavController,
    recordId: Long = -1L
) {
    val database = viewModel.database
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
    var showConsent by remember { mutableStateOf(false) }

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
            analysisError = context.getString(R.string.screen_add_record_error_no_image_or_text)
            return
        }
        analysisJob?.cancel()
        analyzing = true
            analysisProgress = context.getString(R.string.screen_add_record_progress_preparing_data)
        analysisJob = scope.launch(Dispatchers.IO) {
            try {
                analysisProgress = context.getString(R.string.screen_add_record_progress_converting_images)
                val imgs = images.map { bitmapToBase64(it) }
                analysisProgress = context.getString(R.string.screen_add_record_progress_calling_ai)
                analysisResult = analysisUseCase.analyze(noteText, imgs)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                analysisError = e.message ?: context.getString(R.string.screen_add_record_error_recognition_failed)
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
                showConsent = true
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
        else Toast.makeText(context, context.getString(R.string.screen_add_record_error_camera_permission), Toast.LENGTH_SHORT).show()
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
                    r.source?.let { context.getString(R.string.screen_add_record_label_source, it) }
                ).filter { !it.isNullOrBlank() }.joinToString("\n")
                notes = extra
            }
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = if (existingId != null) stringResource(R.string.screen_add_record_title_edit) else stringResource(R.string.screen_add_record_title_add),
                subtitle = stringResource(R.string.screen_add_record_subtitle),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.screen_add_record_icon_back))
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(Icons.Default.Home, stringResource(R.string.screen_add_record_icon_home))
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
            SectionCard(title = stringResource(R.string.screen_add_record_section_family_member)) {
                MemberSelector(
                    members = members,
                    selectedMemberId = selectedMemberId,
                    onSelect = { selectedMemberId = it.id },
                    emptyHint = stringResource(R.string.screen_add_record_empty_members)
                )
            }

            // AI 智能识别区（需要配置 AI 才能使用）
            SectionCard(title = stringResource(R.string.screen_add_record_section_ai_recognition)) {
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
                                    stringResource(R.string.screen_add_record_ai_config_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(R.string.screen_add_record_ai_config_instruction),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                Text(stringResource(R.string.screen_add_record_btn_go_settings))
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
                        ) { Text(stringResource(R.string.screen_add_record_btn_take_photo)) }
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            enabled = !analyzing,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.screen_add_record_btn_select_gallery)) }
                    }

                    if (images.isNotEmpty()) {
                        Text(stringResource(R.string.screen_add_record_images_added, images.size), style = MaterialTheme.typography.bodyMedium)
                    }

                    if (analyzing) {
                        Surface(
                            shape = AppShapes.small,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        stringResource(R.string.screen_add_record_ai_analyzing),
                                        color = MaterialTheme.colorScheme.primary,
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
                        Text(stringResource(R.string.screen_add_record_error_recognition_with_detail, it), color = MaterialTheme.colorScheme.error)
                    }

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text(stringResource(R.string.screen_add_record_label_paste_text)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )

                    if (noteText.isNotBlank()) {
                        OutlinedButton(
                            onClick = { showConsent = true },
                            enabled = !analyzing,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.screen_add_record_btn_analyze_text)) }
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
                                text = stringResource(R.string.screen_add_record_disclaimer),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 基本信息
            SectionCard(title = stringResource(R.string.screen_add_record_section_basic_info)) {
                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text(stringResource(R.string.screen_add_record_label_diagnosis_type)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it },
                    label = { Text(stringResource(R.string.screen_add_record_label_hospital)) },
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
                    Icon(Icons.Default.DateRange, stringResource(R.string.screen_add_record_icon_select_datetime))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.screen_add_record_label_visit_time, onsetTime.format(dateTimeFormatter)))
                }
            }

            // 用药记录
            SectionCard(title = stringResource(R.string.screen_add_record_section_medication)) {
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
                                    Text(stringResource(R.string.screen_add_record_label_medication_number, index + 1), style = MaterialTheme.typography.titleSmall)
                                    OutlinedButton(
                                        onClick = { medItems = medItems.filterIndexed { i, _ -> i != index } },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, stringResource(R.string.screen_add_record_icon_delete), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.screen_add_record_icon_delete))
                                    }
                                }
                                OutlinedTextField(
                                    value = item.name,
                                    onValueChange = { medItems = medItems.updateAt(index) { copy(name = it) } },
                                    label = { Text(stringResource(R.string.screen_add_record_label_medication_name)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = item.dose,
                                        onValueChange = { medItems = medItems.updateAt(index) { copy(dose = it) } },
                                        label = { Text(stringResource(R.string.screen_add_record_label_dosage)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = item.freq,
                                        onValueChange = { medItems = medItems.updateAt(index) { copy(freq = it) } },
                                        label = { Text(stringResource(R.string.screen_add_record_label_frequency)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = item.duration,
                                    onValueChange = { medItems = medItems.updateAt(index) { copy(duration = it) } },
                                    label = { Text(stringResource(R.string.screen_add_record_label_duration)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { medItems = medItems + MedicationItem() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.screen_add_record_btn_add_medication))
                    }
                }
            }

            // 备注
            SectionCard(title = stringResource(R.string.screen_add_record_section_notes)) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.screen_add_record_label_notes)) },
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
                    Text(stringResource(R.string.screen_add_record_btn_cancel))
                }
                Button(
                    onClick = {
                        val selectedMember = members.firstOrNull { it.id == selectedMemberId }
                        if (selectedMemberId == null || selectedMember == null) {
                            error = context.getString(R.string.screen_add_record_error_select_member)
                            return@Button
                        }
                        if (diagnosis.isBlank() || medItems.isEmpty()) {
                            error = context.getString(R.string.screen_add_record_error_fill_required)
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
                                Toast.makeText(context, context.getString(R.string.screen_add_record_toast_save_success), Toast.LENGTH_SHORT).show()
                                navController.navigate("medical_records") {
                                    popUpTo("medical_records") { inclusive = true }
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                                error = e.message ?: context.getString(R.string.screen_add_record_error_save_failed)
                                e.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canSave
                ) {
                    Text(stringResource(R.string.screen_add_record_btn_save))
                }
            }
        }
    }

    if (showConsent) {
        AlertDialog(
            onDismissRequest = { showConsent = false },
            title = { Text(stringResource(R.string.screen_add_record_consent_title)) },
            text = { Text(stringResource(R.string.screen_add_record_consent_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showConsent = false
                    runAnalysis()
                }) {
                    Text(stringResource(R.string.btn_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsent = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

