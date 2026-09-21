package com.example.vastuar.model

data class FurnitureDefinition(
    val id: String,
    val name: String,
    val type: FurnitureType,
    val modelPath: String,
    val preferredSurface: SurfaceType,
    val preferredDirections: Set<Direction>,
    val defaultScale: Float = 1f
)
