package com.example.ntheta

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
fun SystemBroadcastRecvr(systemAction: String, dataScheme: String? = null, onSystemEvent: (intent: Intent?) -> Unit) {
    val ctx = LocalContext.current
    val currentOnSystemEvent by rememberUpdatedState(onSystemEvent)

    DisposableEffect(ctx, systemAction) {
        val intentFilter = IntentFilter(systemAction)
        if(dataScheme != null) {
            intentFilter.addDataScheme(dataScheme)
        }

        val broadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                currentOnSystemEvent(intent)
            }
        }

        ctx.registerReceiver(broadcast, intentFilter)

        onDispose {
            ctx.unregisterReceiver(broadcast)
        }
    }
}