package com.gewuerz.pacer

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.guava.await
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private class HealthState (pref: SharedPreferences?) {
    val installed = pref?.getBoolean("clientInitialized", false) ?: false
    val permissions = pref?.getBoolean("permissionsGranted", false) ?: false
}

private class TaskState private constructor (
    val healthy: Boolean,
    val name: String,
    val nextSchedule: String
) {
    companion object {
        suspend fun get(workManager: WorkManager): TaskState? {
            val infos = workManager.getWorkInfosForUniqueWork("pacerTask").await()
            if (infos.isEmpty()) {
                return null
            }
            val info = infos[0]
            return TaskState(
                listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, WorkInfo.State.SUCCEEDED).contains(info.state),
                info.state.name,
                Instant.ofEpochMilli(info.nextScheduleTimeMillis).atZone(ZoneId.systemDefault()).toLocalDateTime().format(
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            )
        }
    }
}

@Composable
private fun StatusMessage(key: String, good: Boolean?, goodText: String = "OK", badText: String = "ERROR") {
    Row (
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(key)
        if (good == null)
            Text(goodText)
        else if (good)
            Text(
                text = goodText,
                color = MaterialTheme.colorScheme.primary
            )
        else
            Text(
                text = badText,
                color = MaterialTheme.colorScheme.error
            )
    }
}

@Composable
fun Status(pref: SharedPreferences?, workManager: WorkManager?) {
    var healthState by remember { mutableStateOf(HealthState(pref)) }

    DisposableEffect(pref) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, _ ->
            healthState = HealthState(prefs)
        }

        pref?.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            pref?.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val taskState = produceState<TaskState?>(initialValue = null) {
        if (workManager != null) {
            value = TaskState.get(workManager)
        }
    }

    Card (
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Status",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            StatusMessage(
                "Health Connect",
                healthState.installed,
                "INSTALLED",
                "UNAVAILABLE"
            )
            StatusMessage(
                "Permissions",
                healthState.permissions,
                "GRANTED",
                "DENIED"
            )
            StatusMessage(
                "Pacer Task",
                taskState.value?.healthy ?: false,
                taskState.value?.name ?: "UNKNOWN",
                taskState.value?.name ?: "UNKNOWN"
            )
            StatusMessage(
                "Next Execution",
                taskState.value != null,
                taskState.value?.nextSchedule ?: "UNKNOWN",
                "UNKNOWN"
            )
        }
    }
}