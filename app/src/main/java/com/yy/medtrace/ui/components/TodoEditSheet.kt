package com.yy.medtrace.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yy.medtrace.R
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import java.time.LocalDate

/**
 * 待办新增 / 编辑表单。
 *
 * 原先「首页 AddTodoDialog」与「提醒页 EditTodoDialog」是两套几乎逐行重复的实现，
 * 交互细节（日期点击区域、重复选项、按钮文案）已经出现漂移。这里统一为一个组件，
 * 通过 [todo] 是否为 null 区分新增与编辑。
 */
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TodoEditSheet(
    members: List<FamilyMember>,
    modifier: Modifier = Modifier,
    todo: HealthTodo? = null,
    initialCategory: TodoCategory = TodoCategory.OTHER,
    isElderlyMode: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (
        memberId: Long,
        memberName: String,
        content: String,
        dueDate: LocalDate,
        repeatType: RepeatType,
        /** 已本地化的分类文案，直接落库使用 */
        category: String,
        reminderTime: String
    ) -> Unit
) {
    val labels = todoCategoryLabels()

    var content by remember(todo) { mutableStateOf(todo?.content.orEmpty()) }
    var category by remember(todo) {
        mutableStateOf(
            todo?.let { TodoCategory.fromStored(it.category, labels) } ?: initialCategory
        )
    }
    var selectedMemberId by remember(todo, members) {
        mutableStateOf<Long?>(todo?.memberId ?: members.firstOrNull()?.id)
    }
    var dueDate by remember(todo) { mutableStateOf(todo?.dueDate ?: LocalDate.now()) }
    var repeatType by remember(todo) { mutableStateOf(RepeatType.fromKey(todo?.repeatType)) }
    var reminderTime by remember(todo) { mutableStateOf(todo?.reminderTime?.ifBlank { "09:00" } ?: "09:00") }

    val selectedMember = members.firstOrNull { it.id == selectedMemberId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(
                    if (todo == null) R.string.todo_title_add else R.string.todo_title_edit
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.todo_label_content)) },
                placeholder = { Text(stringResource(R.string.todo_label_content_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                stringResource(R.string.todo_label_category),
                style = MaterialTheme.typography.labelMedium
            )
            CategorySelector(
                selected = category,
                onSelect = { category = it },
                isElderlyMode = isElderlyMode
            )

            MemberSelector(
                members = members,
                selectedId = selectedMemberId,
                onSelect = { selectedMemberId = it.id }
            )

            ReminderDateField(date = dueDate, onDateChange = { dueDate = it })

            RepeatField(repeat = repeatType, onRepeatChange = { repeatType = it })

            ReminderTimeField(time = reminderTime, onTimeChange = { reminderTime = it })

            // 分类文案在 Composable 上下文取好，onClick 内无法调用 stringResource
            val categoryText = todoCategoryLabel(category)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.screen_home_cancel)) }
                Button(
                    onClick = {
                        val member = selectedMember ?: return@Button
                        onSave(
                            member.id,
                            member.name,
                            content.trim(),
                            dueDate,
                            repeatType,
                            categoryText,
                            reminderTime
                        )
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    enabled = content.isNotBlank() && selectedMemberId != null
                ) {
                    Text(
                        stringResource(R.string.screen_home_save),
                        fontSize = if (isElderlyMode) 17.sp else 14.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
