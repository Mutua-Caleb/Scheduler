package com.scheduler.calls.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scheduler.calls.data.ScheduledCall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    calls: List<ScheduledCall>,
    onAdd: () -> Unit,
    onEdit: (ScheduledCall) -> Unit,
    onDelete: (ScheduledCall) -> Unit,
    onHistory: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheduled Calls") },
                actions = {
                    IconButton(onClick = onHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        if (calls.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No scheduled calls. Tap + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(calls, key = { it.id }) { call ->
                    CallRow(call, onEdit, onDelete)
                }
            }
        }
    }
}

@Composable
private fun CallRow(
    call: ScheduledCall,
    onEdit: (ScheduledCall) -> Unit,
    onDelete: (ScheduledCall) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(call) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(call.contactName.ifBlank { call.phoneNumber }, fontWeight = FontWeight.Bold)
                Text(call.phoneNumber, style = MaterialTheme.typography.bodySmall)
                Text(formatTime(call.scheduledTimeMillis), style = MaterialTheme.typography.bodyMedium)
                if (call.notes.isNotBlank()) {
                    Text(
                        text = call.notes,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (call.triggered) {
                    Text(
                        text = "Triggered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = { onDelete(call) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

private val displayFormat = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())

private fun formatTime(millis: Long): String = displayFormat.format(Date(millis))
