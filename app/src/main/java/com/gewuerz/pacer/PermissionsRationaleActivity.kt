package com.gewuerz.pacer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.gewuerz.pacer.ui.theme.PacerTheme

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PacerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PrivacyPolicy(
                        padding = innerPadding
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicy(padding: PaddingValues) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "This App doesn't store data.\nSource: Trust me bro!",
            fontSize = 32.sp,
            lineHeight = 48.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PrivacyPolicyPreview() {
    PacerTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            PrivacyPolicy(
                padding = innerPadding
            )
        }
    }
}