package com.whistl.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    onDismissAlarm: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Whistl",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
            }

            // Target selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                IconButton(
                    onClick = { if (state.targetCount > 1) onSetTarget(state.targetCount - 1) },
                    enabled = !state.isListening
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Target")
                }
                listOf(1, 2, 3, 4, 5).forEach { preset ->
                    FilterChip(
                        selected = state.targetCount == preset,
                        onClick = { onSetTarget(preset) },
                        label = { Text("$preset") },
                        enabled = !state.isListening,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
                IconButton(
                    onClick = { onSetTarget(state.targetCount + 1) },
                    enabled = !state.isListening
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Target")
                }
            }

            // Counter ring visualizer
            CounterRing(current = state.currentCount, target = state.targetCount)

            // Audio Confidence Bar
            if (state.isListening) {
                AudioConfidenceBar(confidence = state.currentConfidence, state = state.whistleState)
            } else {
                Spacer(modifier = Modifier.height(30.dp))
            }

            // Whistle History List
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Whistle Log",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.whistleHistory.isEmpty()) {
                        Text(
                            text = "No whistles detected yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        LazyColumn {
                            items(state.whistleHistory.reversed()) { event ->
                                Text(
                                    text = "Whistle #${event.whistleNumber} at ${timeFormat.format(Date(event.timestampMs))} (Conf: ${(event.peakConfidence * 100).toInt()}%)",
                                    fontSize = 13.sp,
                                    color = Color(0xFF00E676),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Primary Start/Stop FAB Button
            Button(
                onClick = { if (state.isListening) onStopListening() else onStartListening() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isListening) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (state.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null
                )
                Text(
                    text = if (state.isListening) "Stop Listening" else "Start Listening",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Target Reached Alarm Dialog
        if (state.isTargetReached) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Cooker Ready!", fontWeight = FontWeight.Bold, color = Color(0xFFFF5252)) },
                text = { Text("Target of ${state.targetCount} whistles reached! Turn off the stove.") },
                confirmButton = {
                    Button(
                        onClick = onDismissAlarm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Text("Stop Alarm", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
