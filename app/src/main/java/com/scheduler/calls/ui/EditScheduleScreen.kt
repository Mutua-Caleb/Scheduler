package com.scheduler.calls.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scheduler.calls.R
import com.scheduler.calls.data.Recurrence
import com.scheduler.calls.data.ScheduledCall
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleScreen(
    existing: ScheduledCall?,
    onSave: (
        name: String,
        phone: String,
        localDateTime: LocalDateTime,
        zoneId: ZoneId,
        notes: String,
        recurrence: Recurrence
    ) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(existing?.contactName ?: "") }
    var phone by remember { mutableStateOf(existing?.phoneNumber ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var localDateTime by remember {
        mutableStateOf(existing?.parsedLocalDateTime() ?: defaultFutureLocal())
    }
    val zoneId = remember(existing) { existing?.zone() ?: ZoneId.systemDefault() }
    var recurrence by remember { mutableStateOf(existing?.recurrence ?: Recurrence.NONE) }
    var recurrenceMenuOpen by remember { mutableStateOf(false) }

    val pickContact = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked = readPickedContact(context, result.data)
            if (picked != null) {
                name = picked.first
                phone = picked.second
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(stringResource(if (existing == null) R.string.edit_title_new else R.string.edit_title_edit))
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_PICK,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                    )
                    pickContact.launch(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Contacts, contentDescription = null)
                Text("  " + stringResource(R.string.pick_from_contacts))
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.label_contact_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.label_phone_number)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.label_notes)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.scheduled_prefix, formatDateTime(localDateTime)))
            Text(
                stringResource(R.string.zone_label, zoneId.id),
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    pickDate(context, localDateTime) { localDateTime = it }
                }) { Text(stringResource(R.string.pick_date)) }
                OutlinedButton(onClick = {
                    pickTime(context, localDateTime) { localDateTime = it }
                }) { Text(stringResource(R.string.pick_time)) }
            }

            Box {
                OutlinedButton(
                    onClick = { recurrenceMenuOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.repeats_prefix, recurrence.label(context)))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = recurrenceMenuOpen,
                    onDismissRequest = { recurrenceMenuOpen = false }
                ) {
                    Recurrence.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label(context)) },
                            onClick = {
                                recurrence = option
                                recurrenceMenuOpen = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = { onSave(name, phone, localDateTime, zoneId, notes, recurrence) },
                    enabled = phone.isNotBlank() &&
                        localDateTime.atZone(zoneId).toInstant().toEpochMilli() > System.currentTimeMillis(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

private fun defaultFutureLocal(): LocalDateTime =
    LocalDateTime.now().withSecond(0).withNano(0).plusHours(1)

private fun pickDate(context: Context, current: LocalDateTime, onResult: (LocalDateTime) -> Unit) {
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onResult(current.withYear(year).withMonth(month + 1).withDayOfMonth(day))
        },
        current.year,
        current.monthValue - 1,
        current.dayOfMonth
    ).show()
}

private fun pickTime(context: Context, current: LocalDateTime, onResult: (LocalDateTime) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute ->
            onResult(current.withHour(hour).withMinute(minute).withSecond(0).withNano(0))
        },
        current.hour,
        current.minute,
        false
    ).show()
}

private val displayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM d yyyy • h:mm a", Locale.getDefault())

private fun formatDateTime(local: LocalDateTime): String = displayFormatter.format(local)

private fun readPickedContact(context: Context, data: Intent?): Pair<String, String>? {
    val uri = data?.data ?: return null
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val displayName = cursor.getString(0).orEmpty()
            val number = cursor.getString(1).orEmpty()
            displayName to number
        } else null
    }
}
