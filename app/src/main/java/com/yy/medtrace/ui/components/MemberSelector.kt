package com.yy.medtrace.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.ui.theme.AppShapes

@Composable
fun MemberSelector(
    members: List<FamilyMember>,
    selectedMemberId: Long?,
    onSelect: (FamilyMember) -> Unit,
    modifier: Modifier = Modifier,
    emptyHint: String = "暂无家庭成员，请先在家庭中添加"
) {
    if (members.isEmpty()) {
        Text(
            text = emptyHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        members.forEach { member ->
            val selected = member.id == selectedMemberId
            Card(
                modifier = Modifier
                    .width(110.dp)
                    .clickable { onSelect(member) },
                shape = AppShapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = if (selected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                border = if (selected)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MemberAvatar(
                        member = member,
                        size = 44.dp,
                        fallbackBackground = if (selected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        fallbackContent = if (selected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    if (member.relation.isNotBlank()) {
                        Text(
                            text = member.relation,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
