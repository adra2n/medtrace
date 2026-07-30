package com.yy.medtrace.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

// 卡片形状（圆角更大）
val CardShape = RoundedCornerShape(20.dp)

// 卡片阴影
val CardElevation = 8.dp

val PrimaryGradient: Brush
    @Composable
    get() = Brush.verticalGradient(
        colors = listOf(Primary, PrimaryGradientEnd),
        startY = 0f,
        endY = 400f
    )

val SoftElevation = 8.dp

@Composable
fun GradientTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            navigationIcon?.let {
                it()
            }
            leadingContent?.let {
                it()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                subtitle?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
fun fadeInItem(
    visible: Boolean = true,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 250, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        label = "fadeIn"
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 8.dp,
        animationSpec = tween(durationMillis = 250, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        label = "fadeInOffset"
    )
    Box(modifier = Modifier.alpha(alpha).offset(y = offsetY)) {
        content()
    }
}

fun Modifier.dashedBorder(
    color: Color,
    width: Dp,
    shape: androidx.compose.foundation.shape.CornerBasedShape,
    dashWidth: Dp = 6.dp,
    gapWidth: Dp = 6.dp
): Modifier = this.drawBehind {
    val density = this@drawBehind
    val radius = shape.topStart.toPx(size, density)
    val half = width.toPx() / 2f
    val path = androidx.compose.ui.graphics.Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = half,
                top = half,
                right = size.width - half,
                bottom = size.height - half,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )
        )
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dashWidth.toPx(), gapWidth.toPx())
            )
        )
    )
}
