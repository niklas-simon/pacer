package com.gewuerz.pacer

import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.work.WorkManager
import com.gewuerz.pacer.ui.theme.PacerTheme

@Composable
fun Main(client: HealthConnectClient? = null, pref: SharedPreferences? = null, workManager: WorkManager? = null, packageManager: PackageManager? = null) {
    PacerTheme {
        Scaffold(modifier = Modifier
            .fillMaxSize()
        ) { innerPadding ->
            Box (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TodaySteps(client, pref, packageManager)
                    Status(pref, workManager)
                    OptionsCard(pref)
                }
            }
        }
    }
}