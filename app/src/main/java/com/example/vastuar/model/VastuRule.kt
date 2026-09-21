package com.example.vastuar.model

data class VastuRule(
    val furnitureType: FurnitureType,
    val allowedDirections: Set<Direction>,
    val description: String
)
