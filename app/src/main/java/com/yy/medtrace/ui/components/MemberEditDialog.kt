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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.util.copyAvatarToInternal
import java.text.SimpleDateFormat
import java.util.*

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

    val avatarBitmap = remember(avatarPath) {
        if (avatarPath.isNotBlank()) {
            BitmapFactory.decodeFile(avatarPath)?.asImageBitmap()
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (member == null) "新增家庭成员" else "编辑家庭成员") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap,
                                    contentDescription = "头像",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Filled.Person, "选择头像", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            }
                        }
                        Text("点击选择头像", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("姓名 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(relation, { relation = it }, label = { Text("关系（如 本人/父亲/子女）") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                DialogSectionTitle("性别")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GENDER_OPTIONS.forEach { g ->
                        FilterChip(
                            selected = gender == g,
                            onClick = { gender = if (gender == g) "" else g },
                            label = { Text(g) }
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
                DialogSectionTitle("血型与医疗信息")
                PresetField("血型", bloodType, { bloodType = it }, BLOOD_PRESETS, onPick = { bloodType = it })
                PresetField("过敏史", allergy, { allergy = it }, ALLERGY_PRESETS, onPick = { allergy = appendCsv(allergy, it) })
                PresetField("慢性病", chronic, { chronic = it }, CHRONIC_PRESETS, onPick = { chronic = appendCsv(chronic, it) })
                PresetField("用药注意", medicationNote, { medicationNote = it }, MEDICATION_PRESETS, onPick = { medicationNote = appendCsv(medicationNote, it) })
                OutlinedTextField(otherNote, { otherNote = it }, label = { Text("其他备注") }, singleLine = false, maxLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
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
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
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

private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

private fun parseBirthdayMillis(value: String): Long? {
    if (value.isBlank()) return null
    return try {
        Calendar.getInstance().apply { time = DATE_FORMAT.parse(value)!! }.timeInMillis
    } catch (_: Exception) {
        null
    }
}

private fun millisToBirthday(millis: Long): String = DATE_FORMAT.format(Date(millis))
