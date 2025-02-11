package com.gewuerz.pacer

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.future
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class PacerTask(appContext: Context, workerParams: WorkerParameters) : ListenableWorker(appContext,
    workerParams
) {
    override fun startWork(): ListenableFuture<Result> {
        try {
            Log.i("pacer", "Started Background Task")

            val pref = applicationContext.getSharedPreferences(applicationContext.packageName, Context.MODE_PRIVATE)

            if (!pref.getBoolean("clientInitialized", false)) {
                Log.w("pacer", "health connect unavailable")
                return Futures.immediateFuture(Result.failure())
            }

            if (!pref.getBoolean("permissionsGranted", false)) {
                Log.w("pacer", "permissions were denied")
                return Futures.immediateFuture(Result.failure())
            }

            val options = Options.fromPref(pref)
            val now = Instant.now().atZone(ZoneId.systemDefault())
            val startTime = now.truncatedTo(ChronoUnit.DAYS).plusSeconds(options.activeFrom * 60L)
            val endTime = now.truncatedTo(ChronoUnit.DAYS).plusSeconds(options.activeTo * 60L)

            if (now < startTime) {
                Log.i("pacer", "it's still too early to add steps")
                return Futures.immediateFuture(Result.success())
            }

            if (now >= endTime) {
                Log.i("pacer", "it's already too late to add steps")
                return Futures.immediateFuture(Result.success())
            }

            val scope = CoroutineScope(Job() + Dispatchers.Default)

            return scope.future {
                try {
                    val zoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())
                    val client = HealthConnectClient.getOrCreate(applicationContext)

                    val currentSteps = getTotalStepsOfToday(client, pref)
                    val neededSteps = options.target - currentSteps

                    if (neededSteps <= 0) {
                        Log.i("pacer", "all steps have been added for today")
                        return@future Result.success()
                    }

                    val nf = NumberFormat.getInstance(java.util.Locale.GERMAN)
                    Log.i("pacer", "still needs " + nf.format(neededSteps) + " steps")

                    val lastExec = Instant.ofEpochMilli(pref.getLong("lastExec", 0L)).atZone(ZoneId.systemDefault())
                    val addFrom = if (startTime < lastExec) lastExec else startTime
                    val timeRemaining = endTime.minusSeconds(addFrom.toEpochSecond())

                    val stepsPerMin = neededSteps * 60 / timeRemaining.toEpochSecond()
                    val minSinceLastExec = now.minusSeconds(addFrom.toEpochSecond()).toEpochSecond() / 60

                    var stepsToAdd = Math.round(minSinceLastExec * stepsPerMin * (Math.random() * .2 + 1f))
                    val stepsAdded = stepsToAdd
                    val partitions = Math.round(minSinceLastExec.toDouble() / 5)
                    val partitionDuration = (minSinceLastExec.toDouble() / partitions).toLong()

                    Log.i("pacer",
                        "adding "
                        + nf.format(stepsAdded)
                        + " steps in "
                        + nf.format(partitions)
                        + " partitions with a duration of "
                        + nf.format(partitionDuration)
                        + " minutes"
                    )

                    val records = ArrayList<StepsRecord>()
                    for (i in 0..<partitions) {
                        val fromMin = partitionDuration * i + Math.round(Math.random())
                        val toMin = partitionDuration * (i + 1) - Math.round(Math.random())
                        val stepsInPartition = if (i == partitions - 1) {
                            stepsToAdd
                        } else {
                            Math.round(stepsToAdd / (partitions - i) * (Math.random() * .2 + .9))
                        }

                        records.add(StepsRecord(
                            Instant.ofEpochMilli(addFrom.toEpochSecond() * 1000 + fromMin * 60_000),
                            zoneOffset,
                            Instant.ofEpochMilli(addFrom.toEpochSecond() * 1000 + toMin * 60_000),
                            zoneOffset,
                            stepsInPartition
                        ))
                        stepsToAdd -= stepsInPartition
                    }

                    client.insertRecords(records)
                    pref.edit().putLong("lastExec", now.toEpochSecond() * 1000).apply()
                    Log.i("pacer", "Inserted Records")
                    return@future Result.success()
                } catch (e: Exception) {
                    Log.e("pacer", "Failed to run task: " + e.message)
                    return@future Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e("pacer", "Failed to run task: " + e.message)
            return Futures.immediateFuture(Result.failure())
        }
    }
}