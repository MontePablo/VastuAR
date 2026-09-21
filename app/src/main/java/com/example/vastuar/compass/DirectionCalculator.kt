package com.example.vastuar.compass

import com.example.vastuar.model.Direction

object DirectionCalculator {

    fun fromDegrees(degrees: Float): Direction {

        val normalized =
            ((degrees % 360f) + 360f) % 360f

        return when {
            normalized >= 337.5f || normalized < 22.5f ->
                Direction.NORTH

            normalized < 67.5f ->
                Direction.NORTH_EAST

            normalized < 112.5f ->
                Direction.EAST

            normalized < 157.5f ->
                Direction.SOUTH_EAST

            normalized < 202.5f ->
                Direction.SOUTH

            normalized < 247.5f ->
                Direction.SOUTH_WEST

            normalized < 292.5f ->
                Direction.WEST

            else ->
                Direction.NORTH_WEST
        }
    }
}