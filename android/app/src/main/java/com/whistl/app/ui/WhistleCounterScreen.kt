package com.whistl.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whistl.app.service.AppSessionState
import com.whistl.app.ui.components.AudioConfidenceBar
import com.whistl.app.ui.components.CounterRing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WhistleCounterScreen(
    state: AppSessionState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSetTarget: (Int) -> Unit,
    onReset: () -> Unit,
    onDismissAlarm: () -> Unit,
    onAddMoreWhistles: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Whistl",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Cooker Whistle Counter",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(onClick = onReset) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Session",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Target Whistle Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TARGET WHISTLES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Minus Button
                        Surface(
                            shape = CircleShape,
                            color = if (!state.isListening && state.targetCount > 1) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(
                                onClick = { if (state.targetCount > 1) onSetTarget(state.targetCount - 1) },
                                enabled = !state.isListening && state.targetCount > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                            }
                        }

                        // Presets Chips (1 to 5 + dynamic custom chip if target > 5)
                        val displayPresets = if (state.targetCount > 5) {
                            listOf(1, 2, 3, 4, 5, state.targetCount)
                        } else {
                            listOf(1, 2, 3, 4, 5)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            displayPresets.forEach { preset ->
                                val isSelected = state.targetCount == preset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable(enabled = !state.isListening) { onSetTarget(preset) }
                                        .padding(horizontal = if (displayPresets.size > 5) 9.dp else 13.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$preset",
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Plus Button
                        Surface(
                            shape = CircleShape,
                            color = if (!state.isListening) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(
                                onClick = { onSetTarget(state.targetCount + 1) },
                                enabled = !state.isListening
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Target Summary Status Pill at Bottom of Card
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (state.targetCount > 5) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (state.targetCount > 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.targetCount > 5) {
                                    "Target set to ${state.targetCount} whistles (custom)"
                                } else {
                                    "Target set to ${state.targetCount} ${if (state.targetCount == 1) "whistle" else "whistles"}"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (state.targetCount > 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Circular Counter Display
            CounterRing(
                current = state.currentCount,
                target = state.targetCount,
                whistleState = state.whistleState,
                isListening = state.isListening
            )

            // Live Audio Visualizer or Idle Helper
            if (state.isListening) {
                AudioConfidenceBar(
                    confidence = state.currentConfidence,
                    state = state.whistleState,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Set your target whistles and tap Start Listening once your cooker is on the stove.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Whistle Event Timeline Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Whistle Timeline",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (state.whistleHistory.isNotEmpty()) {
                            Text(
                                text = "${state.whistleHistory.size} recorded",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (state.whistleHistory.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.isListening) "Listening for whistles..." else "No whistles recorded yet.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
                        LazyColumn {
                            items(state.whistleHistory.reversed()) { event ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00E676).copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${event.whistleNumber}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00C853)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Whistle #${event.whistleNumber}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = timeFormat.format(Date(event.timestampMs)),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action Area with Target Clarification Banner
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (state.targetCount > 5) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.isListening) {
                                "Target: ${state.targetCount} whistles • ${(state.targetCount - state.currentCount).coerceAtLeast(0)} remaining"
                            } else {
                                "Target: ${state.targetCount} whistles • Alarm triggers at count ${state.targetCount}"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.targetCount > 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Primary Start/Stop Hero Button
                val isTargetPassed = state.currentCount >= state.targetCount && state.currentCount > 0
                Button(
                    onClick = {
                        when {
                            state.isListening -> onStopListening()
                            isTargetPassed -> onAddMoreWhistles()
                            else -> onStartListening()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            state.isListening -> Color(0xFFE53935)
                            isTargetPassed -> MaterialTheme.colorScheme.primary
                            else -> Color(0xFF00C853)
                        }
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = when {
                            state.isListening -> Icons.Default.MicOff
                            isTargetPassed -> Icons.Default.Add
                            else -> Icons.Default.Mic
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = when {
                                state.isListening -> "Stop Listening"
                                isTargetPassed -> "+1 More Whistle & Listen"
                                state.currentCount > 0 -> "Continue Listening"
                                else -> "Start Listening"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = when {
                                state.isListening -> "Counting whistles until ${state.targetCount}"
                                isTargetPassed -> "Add 1 whistle • Target: ${state.targetCount + 1}"
                                state.currentCount > 0 -> "${state.targetCount - state.currentCount} whistles remaining"
                                else -> "Ready • Target: ${state.targetCount} whistles"
                            },
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Cooker Ready Celebration Modal
        if (state.isTargetReached) {
            AlertDialog(
                onDismissRequest = {},
                icon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3D00).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFFFF3D00),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Cooker Ready!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF3D00),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Target of ${state.targetCount} whistles completed.\nTurn off the stove flame or add more whistles.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDismissAlarm,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Turn Off Stove & Stop Alarm",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onAddMoreWhistles,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+1 More Whistle & Continue",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}
