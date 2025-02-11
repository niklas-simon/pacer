package com.gewuerz.pacer

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

val PERMISSIONS = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getWritePermission(StepsRecord::class),
    HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val context = applicationContext
        val pref = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        val client = initialize(context)

        setContent {
            Main(client, pref, WorkManager.getInstance(context), context.packageManager)
        }

        pref.edit().putBoolean("clientInitialized", client != null).apply()

        if (client != null) {
            checkPermissions(client, pref)
        }

        startTask(context)
    }

    private fun initialize(context: Context): HealthConnectClient? {
        val availabilityStatus = HealthConnectClient.getSdkStatus(context)
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            return null
        }
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            return null
        }
        return HealthConnectClient.getOrCreate(context)
    }

    @OptIn(ExperimentalFeatureAvailabilityApi::class)
    private fun checkPermissions(healthConnectClient: HealthConnectClient, pref: SharedPreferences) {
        if (healthConnectClient.features
            .getFeatureStatus(
                HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND
            ) != HealthConnectFeatures.FEATURE_STATUS_AVAILABLE) {
            pref.edit().putBoolean("permissionsGranted", false).apply()
            return
        }

        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
        val requestPermissions = registerForActivityResult(requestPermissionActivityContract) { g ->
            pref.edit().putBoolean("permissionsGranted", g.containsAll(PERMISSIONS)).apply()
        }

        val scope = CoroutineScope(Job() + Dispatchers.Main)

        scope.launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(PERMISSIONS)) {
                pref.edit().putBoolean("permissionsGranted", true).apply()
                return@launch
            }

            requestPermissions.launch(PERMISSIONS)
        }
    }

    private fun startTask(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        Log.i("pacer", "Scheduling Background Task")
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "pacerTask",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            PeriodicWorkRequestBuilder<PacerTask>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
        )
        Log.i("pacer", "Background Task scheduled")
    }
}

@Preview(showBackground = true, wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun Preview() {
    Main()
}