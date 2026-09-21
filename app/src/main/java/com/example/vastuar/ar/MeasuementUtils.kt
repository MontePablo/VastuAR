package com.example.vastuar.ar

import com.google.ar.core.Plane
import kotlin.math.round

object MeasurementUtils {

    fun planeWidthMeters(plane: Plane): Float {
        return plane.extentX
    }

    fun planeDepthMeters(plane: Plane): Float {
        return plane.extentZ
    }

    fun roundMeters(value: Float): Float {
        return round(value * 100f) / 100f
    }

    fun metersText(value: Float): String {
        return "${roundMeters(value)} m"
    }

    fun wallWidthMeters(plane: Plane): Float {
        return plane.extentX
    }

    fun wallHeightMeters(plane: Plane): Float {
        return plane.extentZ
    }
    fun uniqueWalls(
        existingWalls: List<Plane>,
        newWalls: List<Plane>
    ): List<Plane> {

        val result = existingWalls.toMutableList()

        for (newWall in newWalls) {

            // Ignore planes that ARCore says have been replaced
            if (newWall.subsumedBy != null) {
                continue
            }

            val newPose = newWall.centerPose

            val isSameWall = result.any { existingWall ->

                if (existingWall.subsumedBy != null) {
                    false
                } else {

                    val existingPose = existingWall.centerPose

                    val dx = newPose.tx() - existingPose.tx()
                    val dz = newPose.tz() - existingPose.tz()

                    val distance = kotlin.math.sqrt(
                        dx * dx + dz * dz
                    )

                    distance < 0.6f
                }
            }

            if (!isSameWall) {
                result.add(newWall)
            }
        }

        return result
    }
}