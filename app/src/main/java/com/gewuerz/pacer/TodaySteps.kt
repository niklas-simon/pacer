package com.gewuerz.pacer

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.text.NumberFormat
import java.time.Instant
import java.time.temporal.ChronoUnit

data class Source(
    val name: String,
    val icon: Drawable,
    val steps: Long
)

data class StepsResult(
    val steps: Long = 0L,
    val sources: List<Source> = ArrayList()
)

val nf: NumberFormat = NumberFormat.getInstance(java.util.Locale.GERMAN)

private suspend fun getTotalAggregation(client: HealthConnectClient, pref: SharedPreferences): AggregationResult? {
    if (!pref.getBoolean("permissionsGranted", false)) {
        return null
    }

    val totalAggregation = client.aggregate(
        AggregateRequest(
            setOf(StepsRecord.COUNT_TOTAL),
            TimeRangeFilter.between(
                Instant.now().truncatedTo(ChronoUnit.DAYS),
                Instant.now()
            )
        )
    )

    return totalAggregation
}

suspend fun getTotalStepsOfToday(client: HealthConnectClient, pref: SharedPreferences): Long {
    return getTotalAggregation(client, pref)?.get(StepsRecord.COUNT_TOTAL) ?: 0L
}

suspend fun getStepsOfToday(client: HealthConnectClient, pref: SharedPreferences, pm: PackageManager): StepsResult {
    if (!pref.getBoolean("permissionsGranted", false)) {
        return StepsResult()
    }

    val totalAggregation = client.aggregate(
        AggregateRequest(
            setOf(StepsRecord.COUNT_TOTAL),
            TimeRangeFilter.between(
                Instant.now().truncatedTo(ChronoUnit.DAYS),
                Instant.now()
            )
        )
    )

    val sources = ArrayList<Source>()
    for (origin in totalAggregation.dataOrigins) {
        val originSteps = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    Instant.now().truncatedTo(ChronoUnit.DAYS),
                    Instant.now()
                ),
                dataOriginFilter = setOf(origin)
            )
        )
        val appInfo = pm.getApplicationInfo(origin.packageName, 0)
        sources.add(Source(
            pm.getApplicationLabel(appInfo).toString(),
            pm.getApplicationIcon(appInfo),
            originSteps[StepsRecord.COUNT_TOTAL] ?: 0L))
    }

    return StepsResult(
        totalAggregation[StepsRecord.COUNT_TOTAL] ?: 0L,
        sources
    )
}

@Composable
fun TodaySteps(client: HealthConnectClient?, pref: SharedPreferences?, pm: PackageManager?) {
    val steps = produceState(
        initialValue = StepsResult()
    ) {
        value = if (client != null && pref != null && pm != null) {
            getStepsOfToday(client, pref, pm)
        } else StepsResult()
    }

    Card (
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = nf.format(steps.value.steps),
                fontSize = 32.sp
            )
            LinearProgressIndicator(
                progress = { steps.value.steps.toFloat() / 10000 },
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
    Card (
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sources",
                fontSize = 24.sp
            )
            if (steps.value.sources.isEmpty()) {
                Text(
                    text = "no data",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            steps.value.sources.map { source ->
                SourceRow(source)
            }
        }
    }
}

@Composable
fun SourceRow(source: Source) {
    Row (
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(source.icon.toBitmap(96, 96).asImageBitmap(), "icon")
        Text(
            text = source.name,
            fontSize = 20.sp
        )
        Text(
            text = nf.format(source.steps),
            fontSize = 20.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}