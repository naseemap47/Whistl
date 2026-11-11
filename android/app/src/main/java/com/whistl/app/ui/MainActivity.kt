package com.whistl.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.MobileAds
import com.whistl.app.service.AppSessionRepository
import com.whistl.app.service.WhistleDetectionService
import com.whistl.app.ui.theme.WhistlTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordAudioGranted) {
            startDetectionService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Google AdMob SDK on background thread
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MainActivity) {}
        }

        setContent {
            WhistlTheme {
                val state by AppSessionRepository.sessionState.collectAsState()

                WhistleCounterScreen(
                    state = state,
                    onStartListening = { checkAndRequestPermissions() },
                    onStopListening = { stopDetectionService() },
                    onSetTarget = { newTarget ->
                        AppSessionRepository.updateState { it.copy(targetCount = newTarget) }
                    },
                    onReset = {
                        AppSessionRepository.resetSession()
                    },
                    onDismissAlarm = {
                        val stopAlarmIntent = Intent(this, WhistleDetectionService::class.java).apply {
                            action = WhistleDetectionService.ACTION_STOP_ALARM
                        }
                        startService(stopAlarmIntent)
                        AppSessionRepository.updateState { it.copy(isTargetReached = false) }
                    },
                    onAddMoreWhistles = {
                        val addWhistleIntent = Intent(this, WhistleDetectionService::class.java).apply {
                            action = WhistleDetectionService.ACTION_ADD_WHISTLE
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(addWhistleIntent)
                        } else {
                            startService(addWhistleIntent)
                        }
                    }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startDetectionService()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startDetectionService() {
        val intent = Intent(this, WhistleDetectionService::class.java).apply {
            action = WhistleDetectionService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopDetectionService() {
        val intent = Intent(this, WhistleDetectionService::class.java).apply {
            action = WhistleDetectionService.ACTION_STOP
        }
        startService(intent)
    }
}
