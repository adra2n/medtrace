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

val PrimaryGradient: Brush
    @Composable
    get() = Brush.verticalGradient(
        colors = listOf(Primary, PrimaryGradientEnd),
        startY = 0f,
        endY = 400f
    )

val SoftElevation = 3.dp

@Composable
fun GradientTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(PrimaryGradient)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            navigationIcon?.let {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    it()
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                subtitle?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
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
