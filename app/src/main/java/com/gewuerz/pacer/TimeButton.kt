package com.gewuerz.pacer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Dismiss")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text("OK")
            }
        },
        text = { content() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    value: Int
) {
    val timePickerState = rememberTimePickerState(
        initialHour = value / 60,
        initialMinute = value % 60,
        is24Hour = true,
    )

    TimePickerDialog(
        onDismiss = { onDismiss() },
        onConfirm = {
            onConfirm(timePickerState.hour * 60 + timePickerState.minute)
        }
    ) {
        TimePicker(
            state = timePickerState
        )
    }
}

@Composable
fun TimeButton(minutes: Int, onChange: (Int) -> Unit) {
    var openDialog by remember { mutableStateOf(false) }

    TextButton (
        onClick = { openDialog = true }
    ) {
        Text(
            String.format(java.util.Locale.GERMAN, "%02d", minutes / 60)
                    + ":" +
                    String.format(java.util.Locale.GERMAN, "%02d", minutes % 60)
        )
    }

    when {
        openDialog -> TimeDialog(
            onConfirm = {
                openDialog = false
                onChange(it)
            },
            onDismiss = { openDialog = false },
            value = minutes
        )
    }
}