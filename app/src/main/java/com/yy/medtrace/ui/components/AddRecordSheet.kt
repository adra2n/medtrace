package com.yy.medtrace.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yy.medtrace.R
import com.yy.medtrace.data.model.FamilyMember
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val VISIT_TYPE_RES = listOf(
    R.string.visit_type_outpatient,
    R.string.visit_type_emergency,
    R.string.visit_type_checkup,
    R.string.visit_type_followup,
    R.string.visit_type_self_medication,
    R.string.visit_type_other
)

/**
 * 新增就诊记录的第一步表单（成员 / 就诊类型 / 病症 / 医院 / 时间）。
 *
 * 首页与病历列表页共用；就诊类型此前是硬编码中文列表，现统一到 strings.xml。
 */
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddRecordBottomSheet(
    onDismiss: () -> Unit,
    onNext: (
        memberId: Long,
        visitType: String,
        diagnosis: String,
        hospital: String,
        onsetTime: String
    ) -> Unit,
    members: List<FamilyMember>
) {
    var selectedMemberId by remember { mutableStateOf(members.firstOrNull()?.id ?: 0L) }
    var diagnosis by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var visitTypeRes by remember { mutableIntStateOf(R.string.visit_type_outpatient) }
    var customVisitType by remember { mutableStateOf("") }

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
            Text(
                text = stringResource(R.string.screen_add_record_title_add),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (members.isNotEmpty()) {
                Text(
                    stringResource(R.string.screen_add_record_section_family_member),
                    style = MaterialTheme.typography.labelMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(members, key = { it.id }) { member ->
                        FilterChip(
                            selected = selectedMemberId == member.id,
                            onClick = { selectedMemberId = member.id },
                            label = { Text(member.name) }
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.screen_add_record_label_visit_type),
                style = MaterialTheme.typography.labelMedium
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(VISIT_TYPE_RES, key = { it }) { resId ->
                    FilterChip(
                        selected = visitTypeRes == resId,
                        onClick = {
                            visitTypeRes = resId
                            if (resId != R.string.visit_type_other) customVisitType = ""
                        },
                        label = { Text(stringResource(resId)) }
                    )
                }
            }
            if (visitTypeRes == R.string.visit_type_other) {
                OutlinedTextField(
                    value = customVisitType,
                    onValueChange = { customVisitType = it },
                    label = { Text(stringResource(R.string.screen_add_record_label_custom_visit_type)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = diagnosis,
                onValueChange = { diagnosis = it },
                label = { Text(stringResource(R.string.screen_add_record_label_diagnosis_detail)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = hospital,
                onValueChange = { hospital = it },
                label = { Text(stringResource(R.string.screen_add_record_label_hospital)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(
                            R.string.screen_add_record_label_visit_time,
                            selectedDate.toString()
                        )
                    )
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                }
            }

            val visitTypeText = stringResource(visitTypeRes)
            Button(
                onClick = {
                    val onsetTime = "${selectedDate}T${selectedTime}"
                    val finalVisitType =
                        if (visitTypeRes == R.string.visit_type_other && customVisitType.isNotBlank()) {
                            customVisitType
                        } else {
                            visitTypeText
                        }
                    onNext(selectedMemberId, finalVisitType, diagnosis, hospital, onsetTime)
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                enabled = members.isNotEmpty()
            ) {
                Text(stringResource(R.string.screen_add_record_btn_next))
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = LocalDate.ofEpochDay(millis / 86_400_000L)
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.screen_home_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.screen_home_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = true
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.screen_home_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.screen_home_cancel))
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}
