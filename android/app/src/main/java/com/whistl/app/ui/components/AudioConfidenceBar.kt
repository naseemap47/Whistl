package com.whistl.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whistl.app.state.WhistleState

@Composable
fun AudioConfidenceBar(
    confidence: Float,
    state: WhistleState,
    modifier: Modifier = Modifier
) {
    val animatedConf by animateFloatAsState(
        targetValue = confidence.coerceIn(0f, 1f),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "confidence"
    )

    val (badgeText, badgeColor, badgeIcon) = when (state) {
        WhistleState.IDLE -> Triple("Listening for sound", MaterialTheme.colorScheme.secondary, "●")
        WhistleState.POSSIBLE -> Triple("Whistle Detected!", Color(0xFFFF9100), "⚡")
        WhistleState.CONFIRMED -> Triple("Confirmed Whistle!", Color(0xFF00E676), "✓")
        WhistleState.COOLDOWN -> Triple("Cooling down / Pressurizing", Color(0xFF00B4D8), "❄")
    }

    val animatedBadgeColor by animateColorAsState(targetValue = badgeColor, label = "badgeColor")

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveAnim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(animatedBadgeColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = badgeText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "${(animatedConf * 100).toInt()}% Conf",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Soundwave animated visualizer bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val barCount = 14
            for (i in 0 until barCount) {
                // Vary bar height based on confidence and wave animation
                val barPhase = ((i % 4) + 1) * 0.25f
                val heightFraction = if (state == WhistleState.POSSIBLE || state == WhistleState.CONFIRMED) {
                    (0.5f + 0.5f * waveAnim * barPhase)
                } else {
                    (0.2f + (animatedConf * 0.7f * barPhase))
                }.coerceIn(0.15f, 1f)

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp * heightFraction)
                        .clip(RoundedCornerShape(2.dp))
                        .background(animatedBadgeColor.copy(alpha = 0.3f + 0.7f * heightFraction))
                )
            }
        }
    }
}
