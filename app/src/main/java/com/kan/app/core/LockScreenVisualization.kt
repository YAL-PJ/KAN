package com.kan.app.core

enum class LockScreenVisualization(val storageValue: Int) {
    Arc(0),
    Pillar(1),
    Constellation(2);

    companion object {
        fun fromStorageValue(value: Int): LockScreenVisualization =
            entries.firstOrNull { it.storageValue == value } ?: Arc
    }
}
