package com.scheduler.calls.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scheduler.calls.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialUrl: String,
    lastSyncMs: Long,
    lastSyncStatus: String,
    syncing: Boolean,
    syncMessage: String,
    onSave: (String) -> Unit,
    onSyncNow: () -> Unit,
    onBack: () -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.settings_explainer),
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.label_server_url)) },
                placeholder = { Text("http://192.168.1.10:8765") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onSave(url) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_save))
                }
                Button(
                    onClick = onSyncNow,
                    enabled = !syncing && url.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.action_sync_now))
                    }
                }
            }

            if (syncMessage.isNotBlank()) {
                Text(syncMessage, style = MaterialTheme.typography.bodySmall)
            }

            if (lastSyncMs > 0) {
                Text(
                    stringResource(R.string.last_sync_at, formatTimestamp(lastSyncMs)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (lastSyncStatus.isNotBlank()) {
                Text(
                    stringResource(R.string.last_sync_status, lastSyncStatus),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                stringResource(R.string.delete_disabled_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

private val tsFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
private fun formatTimestamp(ms: Long): String = tsFormat.format(Date(ms))
