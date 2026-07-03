package com.aegis.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
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

    private val requiredPermissions = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.CAMERA)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        checkSpecialPermissions()
        startAegisService()
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

        requestPermissions()
    }

    private fun requestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkSpecialPermissions()
            startAegisService()
        }
    }

    private fun checkSpecialPermissions() {
        if (!hasUsageStatsPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
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
