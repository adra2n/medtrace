package com.yy.chiyaole.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yy.chiyaole.data.model.FamilyMember

// 统一成员头像：有 avatarPath 显示图片，否则回退首字占位。
@Composable
fun MemberAvatar(
    member: FamilyMember,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fallbackBackground: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    fallbackContent: Color = MaterialTheme.colorScheme.primary
) {
    val bitmap = remember(member.avatarPath) {
        if (member.avatarPath.isNotBlank()) {
            BitmapFactory.decodeFile(member.avatarPath)?.asImageBitmap()
        } else null
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fallbackBackground),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = member.name,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop
            )
        } else {
            val initial = member.name.firstOrNull()?.toString() ?: "?"
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    initial,
                    color = fallbackContent,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
