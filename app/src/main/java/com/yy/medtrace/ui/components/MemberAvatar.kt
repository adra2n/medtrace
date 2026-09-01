package com.yy.medtrace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.yy.medtrace.data.model.FamilyMember
import java.io.File

/**
 * 统一成员头像：有 avatarPath 时加载图片，否则回退首字占位。
 *
 * 之前每次进入可视区都要在 IO 线程重新 `BitmapFactory.decodeFile`，
 * 快速滑动时反复解码同一张图。改用 Coil 后由框架负责内存 / 磁盘缓存、
 * 降采样与生命周期，滚动不再有解码抖动。
 */
@Composable
fun MemberAvatar(
    member: FamilyMember,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fallbackBackground: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    fallbackContent: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fallbackBackground),
        contentAlignment = Alignment.Center
    ) {
        if (member.avatarPath.isBlank()) {
            AvatarInitial(member.name, size, fallbackContent)
        } else {
            val context = LocalContext.current
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(member.avatarPath))
                    .crossfade(true)
                    .build(),
                contentDescription = member.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
                loading = { AvatarInitial(member.name, size, fallbackContent) },
                error = { AvatarInitial(member.name, size, fallbackContent) }
            )
        }
    }
}

@Composable
private fun AvatarInitial(
    name: String,
    size: Dp,
    contentColor: Color
) {
    val initial = name.firstOrNull()?.toString() ?: "?"
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial,
            color = contentColor,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
