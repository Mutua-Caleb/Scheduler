package com.scheduler.calls.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scheduler.calls.R
import com.scheduler.calls.alarm.CallNotifications
import com.scheduler.calls.ui.theme.SchedulerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CallsViewModel by viewModels()

    private val requestRuntimePerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result not strictly required */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CallNotifications.ensureChannels(this)
        requestStartupPermissions()

        setContent {
            SchedulerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val calls by viewModel.calls.collectAsState()
                    val history by viewModel.history.collectAsState()
                    val syncing by viewModel.syncing.collectAsState()
                    val syncMessage by viewModel.syncMessage.collectAsState()

                    var showBatteryDialog by remember {
                        mutableStateOf(shouldPromptBatteryOptimization())
                    }
                    if (showBatteryDialog) {
                        BatteryOptimizationDialog(
                            onConfirm = {
                                markBatteryOptimizationAsked()
                                showBatteryDialog = false
                                launchBatteryOptimizationSettings()
                            },
                            onDismiss = {
                                markBatteryOptimizationAsked()
                                showBatteryDialog = false
                            }
                        )
                    }

                    NavHost(navController = navController, startDestination = "list") {
                        composable("list") {
                            ScheduleListScreen(
                                calls = calls,
                                onAdd = { navController.navigate("edit/0") },
                                onEdit = { navController.navigate("edit/${it.id}") },
                                onDelete = viewModel::delete,
                                onHistory = { navController.navigate("history") },
                                onSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                initialUrl = viewModel.syncSettings.serverUrl,
                                lastSyncMs = viewModel.syncSettings.lastSyncMs,
                                lastSyncStatus = viewModel.syncSettings.lastSyncStatus,
                                syncing = syncing,
                                syncMessage = syncMessage,
                                onSave = viewModel::setServerUrl,
                                onSyncNow = viewModel::syncNow,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("edit/{id}") { entry ->
                            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                            val existing = calls.firstOrNull { it.id == id }
                            EditScheduleScreen(
                                existing = existing,
                                onSave = { name, phone, local, zone, notes, recurrence ->
                                    ensureExactAlarmPermission()
                                    ensureOverlayPermission()
                                    viewModel.saveCall(id, name, phone, local, zone, notes, recurrence)
                                    navController.popBackStack()
                                },
                                onCancel = { navController.popBackStack() }
                            )
                        }
                        composable("history") {
                            HistoryScreen(
                                history = history,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestStartupPermissions() {
        val perms = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        requestRuntimePerms.launch(perms.toTypedArray())
    }

    private fun ensureExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }

    private fun ensureOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun shouldPromptBatteryOptimization(): Boolean {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_BATTERY_ASKED, false)) return false
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun markBatteryOptimizationAsked() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit {
            putBoolean(KEY_BATTERY_ASKED, true)
        }
    }

    @SuppressLint("BatteryLife")
    private fun launchBatteryOptimizationSettings() {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName")
        )
        runCatching { startActivity(intent) }.onFailure {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    companion object {
        private const val PREFS = "scheduler_prefs"
        private const val KEY_BATTERY_ASKED = "battery_optimization_asked"
    }
}

@androidx.compose.runtime.Composable
private fun BatteryOptimizationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.battery_dialog_title)) },
        text = { Text(stringResource(R.string.battery_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.battery_dialog_open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.battery_dialog_not_now))
            }
        }
    )
}
