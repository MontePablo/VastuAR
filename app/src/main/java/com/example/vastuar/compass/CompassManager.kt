package com.example.vastuar.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.roundToInt

class CompassManager(
    context: Context
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val rotationMatrix = FloatArray(9)
    private val adjustedRotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    var azimuth: Float = 0f
        private set

    private var hasInitialHeading = false

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {

        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) {
            return
        }

        SensorManager.getRotationMatrixFromVector(
            rotationMatrix,
            event.values
        )

        val displayRotation =
            windowManager.defaultDisplay.rotation

        when (displayRotation) {

            Surface.ROTATION_0 -> {
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Y,
                    adjustedRotationMatrix
                )
            }

            Surface.ROTATION_90 -> {
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_Y,
                    SensorManager.AXIS_MINUS_X,
                    adjustedRotationMatrix
                )
            }

            Surface.ROTATION_180 -> {
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_MINUS_X,
                    SensorManager.AXIS_MINUS_Y,
                    adjustedRotationMatrix
                )
            }

            Surface.ROTATION_270 -> {
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_MINUS_Y,
                    SensorManager.AXIS_X,
                    adjustedRotationMatrix
                )
            }
        }

        SensorManager.getOrientation(
            adjustedRotationMatrix,
            orientation
        )

        var newHeading =
            Math.toDegrees(
                orientation[0].toDouble()
            ).toFloat()

        if (newHeading < 0f) {
            newHeading += 360f
        }

        azimuth =
            if (!hasInitialHeading) {
                hasInitialHeading = true
                newHeading
            } else {
                smoothHeading(
                    current = azimuth,
                    target = newHeading
                )
            }
    }

    private fun smoothHeading(
        current: Float,
        target: Float
    ): Float {

        var difference = target - current

        if (difference > 180f) {
            difference -= 360f
        }

        if (difference < -180f) {
            difference += 360f
        }

        val smoothed =
            current + difference * 0.15f

        return when {
            smoothed < 0f -> smoothed + 360f
            smoothed >= 360f -> smoothed - 360f
            else -> smoothed
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
    }

    fun roundedAzimuth(): Int {
        return azimuth.roundToInt()
    }
}