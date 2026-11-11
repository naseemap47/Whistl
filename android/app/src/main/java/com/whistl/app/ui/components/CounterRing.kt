package com.whistl.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whistl.app.state.WhistleState

@Composable
fun CounterRing(
    current: Int,
    target: Int,
    whistleState: WhistleState,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress"
    )

    // Pulse animation when listening or whistling
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (whistleState == WhistleState.POSSIBLE) 400 else 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val activeColor by animateColorAsState(
        targetValue = when {
            whistleState == WhistleState.POSSIBLE -> Color(0xFFFFD600)
            whistleState == WhistleState.COOLDOWN -> Color(0xFF00B4D8)
            current >= target && target > 0 -> Color(0xFF00E676)
            isListening -> primaryColor
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "activeColor"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(220.dp)) {
        Canvas(modifier = Modifier.size(210.dp)) {
            val strokeWidth = 16.dp.toPx()

            // Outer ambient pulse glow when listening
            if (isListening) {
                drawCircle(
                    color = activeColor.copy(alpha = pulseAlpha * 0.25f),
                    style = Stroke(width = strokeWidth * 1.6f)
                )
            }

            // Background Track
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokeWidth)
            )

            // Animated Foreground Arc with gradient
            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColor,
                            activeColor,
                            primaryColor
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$current",
                fontSize = 76.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (current >= target && target > 0) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            val remaining = (target - current).coerceAtLeast(0)
            val subtitle = when {
                current >= target && target > 0 -> "Target Reached!"
                remaining == 1 -> "1 whistle remaining"
                remaining > 1 -> "$remaining whistles to go"
                else -> "Target: $target"
            }
            Text(
                text = subtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (current >= target && target > 0) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
