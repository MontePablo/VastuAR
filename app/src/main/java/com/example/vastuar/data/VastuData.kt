package com.example.vastuar.data

import com.example.vastuar.model.Direction
import com.example.vastuar.model.FurnitureType
import com.example.vastuar.model.VastuRule

object VastuData {
    val rules = listOf(

        VastuRule(
            furnitureType = FurnitureType.BED,
            allowedDirections = setOf(
                Direction.SOUTH,
                Direction.WEST
            ),
            description = "Bed is preferred toward South or West."
        ),

        VastuRule(
            furnitureType = FurnitureType.SOFA,
            allowedDirections = setOf(
                Direction.SOUTH,
                Direction.WEST,
                Direction.SOUTH_WEST
            ),
            description = "Sofa is preferred toward South, West or South-West."
        ),

        VastuRule(
            furnitureType = FurnitureType.TABLE,
            allowedDirections = setOf(
                Direction.NORTH,
                Direction.EAST
            ),
            description = "Study/work table is preferred toward North or East."
        ),

        VastuRule(
            furnitureType = FurnitureType.TV,
            allowedDirections = setOf(
                Direction.SOUTH_EAST,
                Direction.SOUTH
            ),
            description = "TV is preferred toward South-East or South."
        ),

        VastuRule(
            furnitureType = FurnitureType.PLANT,
            allowedDirections = setOf(
                Direction.NORTH,
                Direction.EAST,
                Direction.NORTH_EAST
            ),
            description = "Plants are preferred toward North, East or North-East."
        )
    )

    fun ruleFor(type: FurnitureType): VastuRule? {
        return rules.firstOrNull {
            it.furnitureType == type
        }
    }
}