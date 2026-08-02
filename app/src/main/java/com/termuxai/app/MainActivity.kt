package com.termuxai.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.termuxai.app.ui.screens.DashboardScreen
import com.termuxai.app.ui.theme.TermuxAiTheme
import com.termuxai.app.viewmodel.AssistantViewModel
import com.termuxai.app.viewmodel.ConnectionViewModel

/**
 * The launcher Activity. Per the manifest note this replaces ("MainActivity
 * is defined in the UI module (next step)"), this is that next step.
 *
 * Real runtime permission requests happen here — RECORD_AUDIO is mandatory
 * for [com.termuxai.app.core.wakeword.WakeWordListener] to function at all
 * (Vosk cannot open a mic tap without it), and POST_NOTIFICATIONS is
 * required on API 33+ for the two foreground services' persistent
 * notifications to actually show. Neither the WebSocket connection nor the
 * wake word engine is force-started here without the user's explicit tap on
 * a dashboard button — see [DashboardScreen] — except that we do proactively
 * ask for permissions on first launch so the buttons work when tapped rather
 * than silently failing.
 */
class MainActivity : ComponentActivity() {

    private val connectionViewModel: ConnectionViewModel by viewModels {
        ConnectionViewModel.factory(applicationContext)
    }
    private val assistantViewModel: AssistantViewModel by viewModels {
        AssistantViewModel.factory(applicationContext)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Results aren't branched on here — if RECORD_AUDIO is denied, the
           wake word engine's own start() call surfaces the real failure via
           WakeWordService's error notification (see WakeWordService.kt),
           rather than this Activity guessing at the outcome. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRuntimePermissions()
        requestBatteryOptimizationExemption()

        setContent {
            TermuxAiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen(
                        connectionViewModel = connectionViewModel,
                        assistantViewModel = assistantViewModel
                    )
                }
            }
        }
    }

    private fun requestRuntimePermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA // needed for the local torch on/off command
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    /**
     * Real Android system dialog (ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
     * asking the user to exempt this app from Doze/App-Standby battery
     * management. This is necessary because plain Android Doze exemption for
     * foreground services is NOT enough on several OEM skins (MIUI, ColorOS,
     * FuntouchOS, OxygenOS on some versions) -- those add their own
     * background-kill layer on top of stock Android that this system dialog
     * does help with, though it is not a complete guarantee on every device;
     * some OEMs additionally require a manual "autostart" toggle in their
     * own Settings app that no public API can trigger programmatically.
     */
    private fun requestBatteryOptimizationExemption() {
        val powerManager = getSystemService(PowerManager::class.java)
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return

        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
}
