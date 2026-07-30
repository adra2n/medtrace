package com.yy.medtrace.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yy.medtrace.R
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.util.copyAvatarToInternal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ALLERGY_PRESETS = listOf("青霉素", "头孢", "海鲜", "花粉", "鸡蛋", "牛奶")
private val CHRONIC_PRESETS = listOf("高血压", "糖尿病", "心脏病", "哮喘", "痛风", "慢性胃炎")
private val MEDICATION_PRESETS = listOf("餐前服", "餐后服", "忌酒", "定期复查")
private val BLOOD_PRESETS = listOf("A", "B", "AB", "O")
private val GENDER_OPTIONS = listOf("男", "女", "其他")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberEditDialog(
    member: FamilyMember?,
    onDismiss: () -> Unit,
    onSave: (FamilyMember) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(member?.name ?: "") }
    var relation by remember { mutableStateOf(member?.relation ?: "") }
    var gender by remember { mutableStateOf(member?.gender ?: "") }
    var birthday by remember { mutableStateOf(member?.birthday ?: "") }
    var bloodType by remember { mutableStateOf(member?.bloodType ?: "") }
    var allergy by remember { mutableStateOf(member?.allergy ?: "") }
    var chronic by remember { mutableStateOf(member?.chronic ?: "") }
    var medicationNote by remember { mutableStateOf(member?.medicationNote ?: "") }
    var otherNote by remember { mutableStateOf(member?.otherNote ?: "") }
    var avatarPath by remember { mutableStateOf(member?.avatarPath ?: "") }

    var pendingAvatarUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { pendingAvatarUri = it } }

    LaunchedEffect(pendingAvatarUri) {
        pendingAvatarUri?.let { uri ->
            copyAvatarToInternal(context, uri)?.let { path -> avatarPath = path }
            pendingAvatarUri = null
        }
    }

    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(avatarPath) {
        avatarBitmap = if (avatarPath.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(avatarPath)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
        } else null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题
            Text(
                text = if (member == null) "新增家庭成员" else "编辑家庭成员",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 头像选择
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        avatarBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp,
                                contentDescription = "头像",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } ?: Icon(Icons.Filled.Person, "选择头像", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    }
                    Text("点击选择头像", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 基本信息分组
            DialogSectionTitle("👤 基本信息")
            OutlinedTextField(name, { name = it }, label = { Text("姓名 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(relation, { relation = it }, label = { Text("关系（如 本人/父亲/子女）") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            // 个人资料分组
            DialogSectionTitle("🎂 个人资料")
            Text("性别", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GENDER_OPTIONS.forEach { g ->
                    FilterChip(
                        selected = gender == g,
                        onClick = { gender = if (gender == g) "" else g },
                        label = {
                            val icon = when(g) {
                                "男" -> "♂"
                                "女" -> "♀"
                                else -> "⚧"
                            }
                            Text("$icon $g")
                        }
                    )
                }
            }

            val birthdayMillis = parseBirthdayMillis(birthday)
            var showDatePicker by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = birthday,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    label = { Text("生日") },
                    placeholder = { Text("点击选择") },
                    trailingIcon = {
                        Icon(Icons.Filled.Person, "选择生日")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = birthdayMillis ?: System.currentTimeMillis()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                birthday = millisToBirthday(millis)
                            }
                            showDatePicker = false
                        }) { Text("确定") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("取消") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // 健康信息分组
            DialogSectionTitle("🏥 健康信息")
            PresetField("血型", bloodType, { bloodType = it }, BLOOD_PRESETS, onPick = { bloodType = it })
            PresetField("过敏史", allergy, { allergy = it }, ALLERGY_PRESETS, onPick = { allergy = appendCsv(allergy, it) })
            PresetField("慢性病", chronic, { chronic = it }, CHRONIC_PRESETS, onPick = { chronic = appendCsv(chronic, it) })
            PresetField("用药注意", medicationNote, { medicationNote = it }, MEDICATION_PRESETS, onPick = { medicationNote = appendCsv(medicationNote, it) })

            // 备注分组
            DialogSectionTitle("📝 备注")
            OutlinedTextField(otherNote, { otherNote = it }, label = { Text("其他备注") }, singleLine = false, maxLines = 3, modifier = Modifier.fillMaxWidth())
            
            // 底部按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_cancel)) }
                Button(
                    onClick = {
                        val result = FamilyMember(
                            id = member?.id ?: 0,
                            name = name.trim(),
                            relation = relation.trim(),
                            gender = gender,
                            birthday = birthday.trim(),
                            bloodType = bloodType.trim(),
                            allergy = allergy.trim(),
                            chronic = chronic.trim(),
                            medicationNote = medicationNote.trim(),
                            otherNote = otherNote.trim(),
                            isDefault = member?.isDefault ?: false,
                            avatarPath = avatarPath.trim()
                        )
                        onSave(result)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank()
                ) { Text(stringResource(R.string.btn_save)) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DialogSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun PresetField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    presets: List<String>,
    onPick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { p ->
                FilterChip(
                    selected = false,
                    onClick = { onPick(p) },
                    label = { Text(p) }
                )
            }
        }
    }
}

private fun appendCsv(current: String, value: String): String {
    val parts = current.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
    parts.add(value)
    return parts.joinToString(",")
}

private val BIRTHDAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun parseBirthdayMillis(value: String): Long? {
    if (value.isBlank()) return null
    return try {
        LocalDate.parse(value, BIRTHDAY_FORMAT)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (_: Exception) {
        null
    }
}

private fun millisToBirthday(millis: Long): String =
    LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
        .format(BIRTHDAY_FORMAT)
