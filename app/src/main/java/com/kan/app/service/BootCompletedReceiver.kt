package com.kan.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            if (Settings.canDrawOverlays(context)) {
                ScreenTimeService.start(context)
            }
        }
    }
}
