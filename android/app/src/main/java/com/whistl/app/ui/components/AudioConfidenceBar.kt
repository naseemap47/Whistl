package com.whistl.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whistl.app.state.WhistleState

@Composable
fun AudioConfidenceBar(
    confidence: Float,
    state: WhistleState,
    modifier: Modifier = Modifier
) {
    val animatedConf by animateFloatAsState(targetValue = confidence, label = "confidence")
    val barColor = when {
        state == WhistleState.CONFIRMED -> Color(0xFF00E676)
        state == WhistleState.POSSIBLE -> Color(0xFFFFEB3B)
        state == WhistleState.COOLDOWN -> Color(0xFF29B6F6)
        else -> MaterialTheme.colorScheme.primary
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(
            text = "Whistle Confidence: ${(confidence * 100).toInt()}% • State: $state",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LinearProgressIndicator(
            progress = { animatedConf },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}
