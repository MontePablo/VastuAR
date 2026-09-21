package com.example.vastuar.ar

data class ArState(
    val floorDetected: Boolean = false,
    val wallCount: Int = 0,
    val depthSupported: Boolean = false,
    val trackingStable: Boolean = false,
    val scanningComplete: Boolean = false
) {

    val readyForPlacement: Boolean
        get() =
            floorDetected &&
                    wallCount >= 1 &&
                    trackingStable
}