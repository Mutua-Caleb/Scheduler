package com.scheduler.calls.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    private val viewModel: CallsViewModel by viewModels()

    private val requestRuntimePerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result not strictly required */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestStartupPermissions()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val calls by viewModel.calls.collectAsState()

                    NavHost(navController = navController, startDestination = "list") {
                        composable("list") {
                            ScheduleListScreen(
                                calls = calls,
                                onAdd = { navController.navigate("edit/0") },
                                onEdit = { navController.navigate("edit/${it.id}") },
                                onDelete = viewModel::delete
                            )
                        }
                        composable("edit/{id}") { entry ->
                            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                            val existing = calls.firstOrNull { it.id == id }
                            EditScheduleScreen(
                                existing = existing,
                                onSave = { name, phone, time, notes ->
                                    ensureExactAlarmPermission()
                                    ensureOverlayPermission()
                                    viewModel.saveCall(id, name, phone, time, notes)
                                    navController.popBackStack()
                                },
                                onCancel = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestStartupPermissions() {
        val perms = mutableListOf(Manifest.permission.CALL_PHONE)
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
}
