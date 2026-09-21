package com.example.vastuar.ar

import com.example.vastuar.data.VastuData
import com.example.vastuar.model.Direction
import com.example.vastuar.model.FurnitureType
import com.google.ar.core.Pose
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.atan2

object VastuPlacementEngine {

    /**
     * Calculates where magnetic North lies inside the ARCore world.
     *
     * ARCore uses:
     *   X = right
     *   Y = up
     *   Z = backward
     *
     * Camera forward is therefore -Z.
     */
    fun calculateNorthAngle(
        sensorPose: Pose,
        compassHeading: Float
    ): Double {

        /*
         * Android compass azimuth describes the direction
         * of the device's +Y axis.
         *
         * ARCore's getAndroidSensorPose() gives us that same
         * Android Sensor Coordinate System inside ARCore world space.
         */
        val deviceTop = sensorPose.rotateVector(
            floatArrayOf(0f, 1f, 0f)
        )

        val x = deviceTop[0].toDouble()
        val z = deviceTop[2].toDouble()

        val arSensorAngle = kotlin.math.atan2(
            x,
            -z
        )

        val compassAngle =
            Math.toRadians(compassHeading.toDouble())

        var northAngle = arSensorAngle - compassAngle

        while (northAngle > Math.PI) {
            northAngle -= 2.0 * Math.PI
        }

        while (northAngle < -Math.PI) {
            northAngle += 2.0 * Math.PI
        }

        return northAngle
    }
    fun calculateArCameraAngle(
        sensorPose: Pose
    ): Double {

        val deviceTop = sensorPose.rotateVector(
            floatArrayOf(0f, 1f, 0f)
        )

        val x = deviceTop[0].toDouble()
        val z = deviceTop[2].toDouble()

        return kotlin.math.atan2(x, -z)
    }
    fun directionVector(
        direction: Direction,
        northAngle: Double
    ): FloatArray {

        val directionAngle = when (direction) {

            Direction.NORTH -> 0.0
            Direction.NORTH_EAST -> 45.0
            Direction.EAST -> 90.0
            Direction.SOUTH_EAST -> 135.0
            Direction.SOUTH -> 180.0
            Direction.SOUTH_WEST -> 225.0
            Direction.WEST -> 270.0
            Direction.NORTH_WEST -> 315.0
        }

        val angle =
            northAngle +
                    Math.toRadians(directionAngle)

        return floatArrayOf(
            sin(angle).toFloat(),
            0f,
            (-cos(angle)).toFloat()
        )
    }
    fun directionFromPosition(
        objectWorldX: Float,
        objectWorldZ: Float,
        roomCenterWorldX: Float,
        roomCenterWorldZ: Float,
        northAngle: Double
    ): Direction? {

        val dx =
            objectWorldX - roomCenterWorldX

        val dz =
            objectWorldZ - roomCenterWorldZ

        val distance =
            kotlin.math.sqrt(
                dx * dx + dz * dz
            )

        if (distance < 0.05f) {
            return null
        }

        /*
         * Heading of the object from the room center
         * in the AR/world coordinate system.
         */
        val worldAngle =
            atan2(
                dx.toDouble(),
                -dz.toDouble()
            )

        /*
         * Convert world angle into real-world angle
         * relative to North.
         */
        var realAngle =
            worldAngle - northAngle

        while (realAngle < 0.0) {
            realAngle += 2.0 * Math.PI
        }

        while (realAngle >= 2.0 * Math.PI) {
            realAngle -= 2.0 * Math.PI
        }

        /*
         * Divide the compass into eight 45° sectors.
         */
        val sector =
            kotlin.math.floor(
                (Math.toDegrees(realAngle) + 22.5) / 45.0
            ).toInt() % 8

        return when (sector) {
            0 -> Direction.NORTH
            1 -> Direction.NORTH_EAST
            2 -> Direction.EAST
            3 -> Direction.SOUTH_EAST
            4 -> Direction.SOUTH
            5 -> Direction.SOUTH_WEST
            6 -> Direction.WEST
            7 -> Direction.NORTH_WEST
            else -> null
        }
    }

    fun isVastuCompliant(
        type: FurnitureType,
        objectWorldX: Float,
        objectWorldZ: Float,
        roomCenterWorldX: Float,
        roomCenterWorldZ: Float,
        northAngle: Double
    ): Boolean {

        val direction =
            directionFromPosition(
                objectWorldX = objectWorldX,
                objectWorldZ = objectWorldZ,
                roomCenterWorldX = roomCenterWorldX,
                roomCenterWorldZ = roomCenterWorldZ,
                northAngle = northAngle
            ) ?: return false

        val rule =
            VastuData.ruleFor(type)
                ?: return false

        return direction in rule.allowedDirections
    }
    fun calculateFurnitureRotationTowardCamera(
        cameraPose: Pose,
        objectWorldX: Float,
        objectWorldZ: Float,
        anchorPose: Pose
    ): Float {

        // Direction from furniture -> user.
        val dx =
            cameraPose.tx() - objectWorldX

        val dz =
            cameraPose.tz() - objectWorldZ

        val distance =
            kotlin.math.sqrt(
                dx * dx +
                        dz * dz
            )

        if (distance < 0.001f) {
            return 0f
        }

        val desiredWorldYaw =
            Math.toDegrees(
                atan2(
                    dx.toDouble(),
                    -dz.toDouble()
                )
            )

        // Anchor's local -Z direction in world space.
        val anchorForward =
            anchorPose.rotateVector(
                floatArrayOf(
                    0f,
                    0f,
                    -1f
                )
            )

        val anchorYaw =
            Math.toDegrees(
                atan2(
                    anchorForward[0].toDouble(),
                    -anchorForward[2].toDouble()
                )
            )

        /*
         * Model front is assumed to be local -Z.
         *
         * worldYaw = anchorYaw - localRotation
         *
         * Therefore:
         *
         * localRotation = anchorYaw - desiredWorldYaw
         */
        var localRotation =
//            anchorYaw - desiredWorldYaw
            anchorYaw - desiredWorldYaw + 180.0

        while (localRotation > 180.0) {
            localRotation -= 360.0
        }

        while (localRotation < -180.0) {
            localRotation += 360.0
        }

        return localRotation.toFloat()
    }

    fun calculateWallFacingDirection(
        wallPose: Pose,
        cameraPose: Pose
    ): FloatArray {

        // ARCore vertical plane normal.
        val wallNormal =
            wallPose.rotateVector(
                floatArrayOf(
                    0f,
                    1f,
                    0f
                )
            )

        // Direction from wall -> user.
        val toCamera = floatArrayOf(
            cameraPose.tx() - wallPose.tx(),
            0f,
            cameraPose.tz() - wallPose.tz()
        )

        val distance =
            kotlin.math.sqrt(
                toCamera[0] * toCamera[0] +
                        toCamera[2] * toCamera[2]
            )

        if (distance < 0.001f) {
            return floatArrayOf(
                0f,
                0f,
                -1f
            )
        }

        toCamera[0] /= distance
        toCamera[2] /= distance

        var facingX = wallNormal[0]
        var facingZ = wallNormal[2]

        /*
         * ARCore can give either normal direction.
         *
         * Make sure the normal points toward the
         * room/user side of the wall.
         */
        val dot =
            facingX * toCamera[0] +
                    facingZ * toCamera[2]

        if (dot < 0f) {
            facingX = -facingX
            facingZ = -facingZ
        }

        return floatArrayOf(
            facingX,
            0f,
            facingZ
        )
    }

    fun calculateTvRotationForWall(
        wallPose: Pose,
        cameraPose: Pose,
        anchorPose: Pose
    ): Float {

        val facingDirection =
            calculateWallFacingDirection(
                wallPose = wallPose,
                cameraPose = cameraPose
            )

        val desiredWorldYaw =
            Math.toDegrees(
                atan2(
                    facingDirection[0].toDouble(),
                    -facingDirection[2].toDouble()
                )
            )

        val anchorForward =
            anchorPose.rotateVector(
                floatArrayOf(
                    0f,
                    0f,
                    -1f
                )
            )

        val anchorYaw =
            Math.toDegrees(
                atan2(
                    anchorForward[0].toDouble(),
                    -anchorForward[2].toDouble()
                )
            )

        var localRotation =
//            anchorYaw - desiredWorldYaw
            anchorYaw - desiredWorldYaw + 180.0


        while (localRotation > 180.0) {
            localRotation -= 360.0
        }

        while (localRotation < -180.0) {
            localRotation += 360.0
        }

        return localRotation.toFloat()
    }

    fun chooseDirection(
        type: FurnitureType,
        usedDirections: Set<Direction>
    ): Direction? {

        val rule =
            VastuData.ruleFor(type)
                ?: return null

        return rule.allowedDirections
            .firstOrNull { it !in usedDirections }
            ?: rule.allowedDirections.firstOrNull()
    }

    fun positionForDirection(
        direction: Direction,
        northAngle: Double,
        roomWidth: Float,
        roomDepth: Float
    ): PositionResult {

        val directionAngle = when (direction) {

            Direction.NORTH -> 0.0

            Direction.NORTH_EAST -> 45.0

            Direction.EAST -> 90.0

            Direction.SOUTH_EAST -> 135.0

            Direction.SOUTH -> 180.0

            Direction.SOUTH_WEST -> 225.0

            Direction.WEST -> 270.0

            Direction.NORTH_WEST -> 315.0
        }

        val angle =
            northAngle +
                    Math.toRadians(directionAngle)

        val maxDistance =
            min(roomWidth, roomDepth) * 0.35f

        val x =
            (sin(angle) * maxDistance)
                .toFloat()

        val z =
            (-cos(angle) * maxDistance)
                .toFloat()

        return PositionResult(
            x = x,
            y = 0f,
            z = z
        )
    }

    data class PositionResult(
        val x: Float,
        val y: Float,
        val z: Float
    )
    fun positionNorth(
        northAngle: Double,
        distance: Float
    ): PositionResult {

        val x =
            (sin(northAngle) * distance)
                .toFloat()

        val z =
            (-cos(northAngle) * distance)
                .toFloat()

        return PositionResult(
            x = x,
            y = 0f,
            z = z
        )
    }

}