package com.mogalab.fliplock.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FocusSensorManager(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val proximitySensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isDark = false
    private var isFaceDown = false

    private val _isFocusPositionFlow = MutableStateFlow(false)
    val isFocusPosition: StateFlow<Boolean> = _isFocusPositionFlow.asStateFlow()

    private val _isDarkFlow = MutableStateFlow(false)
    val isDarkFlow: StateFlow<Boolean> = _isDarkFlow.asStateFlow()

    private val _isFaceDownFlow = MutableStateFlow(false)
    val isFaceDownFlow: StateFlow<Boolean> = _isFaceDownFlow.asStateFlow()

    fun start() {
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        isDark = false
        isFaceDown = false
        _isFocusPositionFlow.value = false
        _isDarkFlow.value = false
        _isFaceDownFlow.value = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val maxRange = proximitySensor?.maximumRange ?: 1f
                isDark = event.values[0] < maxRange
                _isDarkFlow.value = isDark
            }
            Sensor.TYPE_ACCELEROMETER -> {
                // When face-down flat on a surface, gravity acts on Z axis as ~-9.8 m/s²
                isFaceDown = event.values[2] < -9.0f
                _isFaceDownFlow.value = isFaceDown
            }
        }
        _isFocusPositionFlow.value = isDark && isFaceDown
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
