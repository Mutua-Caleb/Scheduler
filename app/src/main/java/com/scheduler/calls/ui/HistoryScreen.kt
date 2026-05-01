package com.scheduler.calls.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.scheduler.calls.data.CallEvent
import com.scheduler.calls.data.CallOutcome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(history: List<CallEvent>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call history") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No call history yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.id }) { event -> EventRow(event) }
            }
        }
    }
}

@Composable
private fun EventRow(event: CallEvent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                event.contactName.ifBlank { event.phoneNumber },
                fontWeight = FontWeight.Bold
            )
            Text(event.phoneNumber, style = MaterialTheme.typography.bodySmall)
            Text(formatTimestamp(event.firedAtMillis), style = MaterialTheme.typography.bodyMedium)
            Text(
                outcomeLabel(event.outcome),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (event.notes.isNotBlank()) {
                Text(
                    event.notes,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun outcomeLabel(o: CallOutcome): String = when (o) {
    CallOutcome.CALLED -> "Called"
    CallOutcome.AUTO_FIRED -> "Auto-dialed"
    CallOutcome.SNOOZED -> "Snoozed"
    CallOutcome.CANCELLED -> "Cancelled"
}

private val historyFormat = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
private fun formatTimestamp(millis: Long): String = historyFormat.format(Date(millis))
