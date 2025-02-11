package com.gewuerz.pacer

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.lang.Long.parseLong
import java.text.NumberFormat

data class Options (val target: Long, val activeFrom: Int, val activeTo: Int) {
    companion object {
        fun fromPref(pref: SharedPreferences?): Options {
            val target = pref?.getLong("target", 10_000L) ?: 10_000L
            val activeFrom = pref?.getInt("activeFrom", 360) ?: 360
            val activeTo = pref?.getInt("activeTo", 1320) ?: 1320
            return Options(target, activeFrom, activeTo)
        }
    }

    fun save(pref: SharedPreferences?) {
        pref?.edit()
            ?.putLong("target", target.toLong())
            ?.putInt("activeFrom", activeFrom)
            ?.putInt("activeTo", activeTo)
            ?.apply()
    }
}

@Composable
fun OptionsCard(pref: SharedPreferences?) {
    var options by remember { mutableStateOf(Options.fromPref(pref)) }
    var modified by remember { mutableStateOf(false) }
    val nf = NumberFormat.getInstance(java.util.Locale.GERMAN)

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
                text = "Options",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Target")
                Text(nf.format(options.target))
            }
            Slider(
                value = options.target.toFloat() / 100_000,
                onValueChange = {
                    val value = Math.round(it * 100).toLong() * 1000
                    if (options.target != value) {
                        options = options.copy(target = value)
                        modified = true
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text("Active Range")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeButton(
                    minutes = options.activeFrom,
                    onChange = {
                        if (options.activeFrom != it) {
                            options = options.copy(activeFrom = it)
                            modified = true
                        }
                    }
                )
                Text("to")
                TimeButton(
                    minutes = options.activeTo,
                    onChange = {
                        if (options.activeTo != it) {
                            options = options.copy(activeTo = it)
                            modified = true
                        }
                    }
                )
            }
            TextButton (
                onClick = {
                    options.save(pref)
                    modified = false
                },
                modifier = Modifier
                    .fillMaxWidth(),
                enabled = modified
            ) {
                Text("Save")
            }
        }
    }
}