package com.kan.app.core

enum class LockTimerMode(val storageValue: Int) {
    Chronometer(0),
    FullScreen(1),
    Banner(2);

    companion object {
        fun fromStorageValue(value: Int): LockTimerMode =
            entries.firstOrNull { it.storageValue == value } ?: Chronometer
    }
}
