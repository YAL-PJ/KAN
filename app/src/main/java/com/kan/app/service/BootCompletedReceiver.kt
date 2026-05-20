package com.kan.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kan.app.data.ScreenTimeRepository

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (!ScreenTimeRepository.get(context).isOnboardingCompleted()) return
        ScreenTimeService.start(context)
    }
}
