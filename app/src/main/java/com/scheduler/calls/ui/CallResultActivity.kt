package com.scheduler.calls.ui

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scheduler.calls.R
import com.scheduler.calls.alarm.CallNotifications
import com.scheduler.calls.alarm.CallResultReceiver
import com.scheduler.calls.data.AppDatabase
import com.scheduler.calls.data.CallEvent
import com.scheduler.calls.data.CallRepository
import com.scheduler.calls.data.CallResult
import com.scheduler.calls.sync.SyncWorker
import com.scheduler.calls.ui.theme.SchedulerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallResultActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eventId = intent.getLongExtra(CallResultReceiver.EXTRA_EVENT_ID, -1L)

        setContent {
            SchedulerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (eventId < 0) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.result_event_missing))
                        }
                    } else {
                        ResultEntry(eventId = eventId, onDone = { finish() })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultEntry(eventId: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var event by remember { mutableStateOf<CallEvent?>(null) }
    var selected by remember { mutableStateOf<CallResult?>(null) }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        val db = AppDatabase.get(context)
        val loaded = withContext(Dispatchers.IO) { db.callEventDao().getById(eventId) }
        event = loaded
        selected = loaded?.callResult
        notes = loaded?.resultNotes.orEmpty()
    }

    val current = event
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    if (current == null) stringResource(R.string.result_loading)
                    else stringResource(
                        R.string.result_for_contact,
                        current.contactName.ifBlank { current.phoneNumber }
                    )
                )
            })
        }
    ) { padding ->
        if (current == null) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.result_question),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResultChip(R.string.result_reached, CallResult.REACHED, selected) { selected = it }
                ResultChip(R.string.result_no_answer, CallResult.NO_ANSWER, selected) { selected = it }
                ResultChip(R.string.result_voicemail, CallResult.VOICEMAIL, selected) { selected = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultChip(R.string.result_follow_up, CallResult.FOLLOW_UP, selected) { selected = it }
                ResultChip(R.string.result_other, CallResult.OTHER, selected) { selected = it }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.label_what_happened)) },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_skip))
                }
                Button(
                    enabled = selected != null && !saving,
                    onClick = {
                        val choice = selected ?: return@Button
                        saving = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val db = AppDatabase.get(context)
                                val repo = CallRepository(
                                    db.scheduledCallDao(),
                                    db.callEventDao()
                                )
                                repo.setCallResult(eventId, choice, notes.trim())
                            }
                            val nm = context
                                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            nm.cancel(CallNotifications.resultNotificationId(eventId))
                            SyncWorker.enqueueOneShot(context)
                            onDone()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultChip(
    labelRes: Int,
    value: CallResult,
    selected: CallResult?,
    onSelect: (CallResult) -> Unit
) {
    val isSelected = selected == value
    AssistChip(
        onClick = { onSelect(value) },
        label = { Text(stringResource(labelRes)) },
        colors = if (isSelected) {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primary,
                labelColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            AssistChipDefaults.assistChipColors()
        }
    )
}
