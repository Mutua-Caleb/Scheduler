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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.scheduler.calls.data.ScheduledCall
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleScreen(
    existing: ScheduledCall?,
    onSave: (name: String, phone: String, time: Long, notes: String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(existing?.contactName ?: "") }
    var phone by remember { mutableStateOf(existing?.phoneNumber ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var timeMillis by remember {
        mutableStateOf(
            existing?.scheduledTimeMillis ?: defaultFutureTime()
        )
    }

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
            TopAppBar(title = { Text(if (existing == null) "New Call" else "Edit Call") })
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
                Text("  Pick from contacts")
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Contact name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (shown when calling)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Scheduled: ${formatDateTime(timeMillis)}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    pickDate(context, timeMillis) { newMillis -> timeMillis = newMillis }
                }) {
                    Text("Pick date")
                }
                OutlinedButton(onClick = {
                    pickTime(context, timeMillis) { newMillis -> timeMillis = newMillis }
                }) {
                    Text("Pick time")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSave(name, phone, timeMillis, notes) },
                    enabled = phone.isNotBlank() && timeMillis > System.currentTimeMillis(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

private fun defaultFutureTime(): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.HOUR_OF_DAY, 1)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun pickDate(context: android.content.Context, current: Long, onResult: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = current }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            onResult(cal.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun pickTime(context: android.content.Context, current: Long, onResult: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = current }
    TimePickerDialog(
        context,
        { _, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            onResult(cal.timeInMillis)
        },
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE),
        false
    ).show()
}

private val dateTimeFormat = SimpleDateFormat("EEE, MMM d yyyy • h:mm a", Locale.getDefault())

private fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

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
