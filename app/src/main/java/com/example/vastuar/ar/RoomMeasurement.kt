package com.example.vastuar.ar

data class RoomMeasurement(
    val id: String,
    val widthMeters: Float,
    val heightMeters: Float?,
    val label: String,
    val isFloor: Boolean
)
