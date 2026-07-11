package com.aegis.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.provider.Settings
import timber.log.Timber
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.aegis.agents.GuardianCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.aegis.data.repository.LearningRepository
import com.aegis.data.repository.SafetyRepository
import com.aegis.data.repository.SettingsRepository
import com.aegis.data.repository.ThreatRepository
import com.aegis.services.foreground.AegisForegroundService
import com.aegis.ui.navigation.AegisNavGraph
import com.aegis.ui.theme.AegisTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

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

    private val requiredPermissions = buildList {
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

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            checkSpecialPermissions()
            startAegisService()
            
            // Try to load the AI engine once permissions are granted
            lifecycleScope.launch {
                guardianCore.gemmaEngine?.loadModel()
            }
        }

    private val _intentFlow = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _intentFlow.value = intent
        enableEdgeToEdge()

        setContent {
            val currentIntent by _intentFlow.collectAsState(initial = null)
            val intentToUse = currentIntent

            val darkModeSetting by settingsRepository
                .observeString(SettingsRepository.KEY_DARK_MODE)
                .collectAsState(initial = null)

            val darkTheme =
                darkModeSetting?.value?.toBooleanStrictOrNull() ?: false

            AegisTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    
                    // Handle deep link from overlay
                    LaunchedEffect(intentToUse) {
                        val threatId = intentToUse?.getStringExtra("nav_to_threat_id")
                        if (threatId != null || intentToUse?.action == "com.aegis.ACTION_NAV_TO_THREAT") {
                            val id = threatId ?: intentToUse?.getStringExtra("nav_to_threat_id")
                            if (id != null) {
                                navController.navigate("threat_detail/$id")
                                // Clear intent so it doesn't re-trigger on rotation
                                _intentFlow.value = null
                            }
                        }
                    }

                    AegisNavGraph(
                        navController = navController,
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _intentFlow.value = intent
    }

    override fun onResume() {
        super.onResume()
        // If the user just came back from settings after granting "All Files Access",
        // try to load the model again automatically.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            lifecycleScope.launch {
                guardianCore.gemmaEngine?.loadModel()
            }
        }
    }

    private fun requestPermissions() {

        val permissionsToRequest = requiredPermissions.filterNot(::hasPermission)

        if (permissionsToRequest.isEmpty()) {
            checkSpecialPermissions()
            startAegisService()
            return
        }

        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    private fun checkSpecialPermissions() {

        if (!hasUsageStatsPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun hasUsageStatsPermission(): Boolean {

        val appOps =
            getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        val mode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    packageName
                )
            }

        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun startAegisService() {

        val serviceIntent = Intent(this, AegisForegroundService::class.java)

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }.onFailure {
            Timber.tag(TAG).e(it, "Failed to start AEGIS foreground service.")
        }
    }
}