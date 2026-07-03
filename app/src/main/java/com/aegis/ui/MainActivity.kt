package com.aegis.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.aegis.agents.GuardianCore
import com.aegis.data.repository.*
import com.aegis.services.foreground.AegisForegroundService
import com.aegis.ui.navigation.AegisNavGraph
import com.aegis.ui.theme.AegisTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var guardianCore: GuardianCore
    @Inject
    lateinit var threatRepository: ThreatRepository
    @Inject
    lateinit var safetyRepository: SafetyRepository
    @Inject
    lateinit var learningRepository: LearningRepository
    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startAegisService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by settingsRepository.observeString(
                SettingsRepository.KEY_DARK_MODE
            ).collectAsState(initial = null)

            val darkTheme = isDarkMode?.value?.toBooleanStrictOrNull() ?: false

            AegisTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AegisNavGraph(
                        guardianCore = guardianCore,
                        threatRepository = threatRepository,
                        safetyRepository = safetyRepository,
                        learningRepository = learningRepository,
                        settingsRepository = settingsRepository
                    )
                }
            }
        }

        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startAegisService()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startAegisService()
        }
    }

    private fun startAegisService() {
        val serviceIntent = Intent(this, AegisForegroundService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
