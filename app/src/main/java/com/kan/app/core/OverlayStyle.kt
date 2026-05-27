package com.kan.app.core

enum class OverlayStyle(val storageValue: Int) {
    Bar(0),
    Dots(1),
    Ring(2);

    companion object {
        fun fromStorageValue(value: Int): OverlayStyle =
            entries.firstOrNull { it.storageValue == value } ?: Bar
    }
}
