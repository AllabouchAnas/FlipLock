package com.mogalab.fliplock.ui

import android.app.Application
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mogalab.fliplock.data.FocusPreferencesRepository
import com.mogalab.fliplock.data.FocusStats
import com.mogalab.fliplock.sensor.FocusSensorManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SessionPhase {
    IDLE,       // Waiting to start
    WAITING,    // Session started, waiting for device flip
    ACTIVE,     // Device is face-down: focus mode
    BROKEN,     // Device lifted: 10-second grace period countdown
    COMPLETED,  // Session finished successfully
    FAILED      // Session failed (grace period expired)
}

data class FocusUiState(
    val phase: SessionPhase = SessionPhase.IDLE,
    val selectedDurationMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val graceCountdownSeconds: Int = 10,
    val isDeviceLocked: Boolean = false,
    val isMuted: Boolean = false,
    val stats: FocusStats = FocusStats(),
    val isSensorTestCompleted: Boolean = false
) {
    val formattedTime: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return "%02d:%02d".format(mins, secs)
        }
    val progressFraction: Float
        get() {
            val total = selectedDurationMinutes * 60
            return if (total == 0) 0f else 1f - (remainingSeconds.toFloat() / total)
        }
}

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    val sensorManager = FocusSensorManager(application)
    private val repository = FocusPreferencesRepository(application)

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = application.getSystemService(VibratorManager::class.java)
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Vibrator::class.java)!!
        }
    }

    private val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    private var originalDndFilter: Int? = null

    fun isDndPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
    }

    private fun enableDnd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isDndPermissionGranted()) {
            try {
                originalDndFilter = notificationManager.currentInterruptionFilter
                notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun disableDnd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isDndPermissionGranted()) {
            try {
                val filterToRestore = originalDndFilter ?: android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                notificationManager.setInterruptionFilter(filterToRestore)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun vibrateSuccess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startCalibration() {
        sensorManager.start()
    }

    fun stopCalibration() {
        if (_uiState.value.phase == SessionPhase.IDLE) {
            sensorManager.stop()
        }
    }

    fun completeSensorTest() {
        viewModelScope.launch {
            repository.markSensorTestCompleted()
        }
    }

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var graceJob: Job? = null
    private var ringtone: android.media.Ringtone? = null

    init {
        // Observe stats from DataStore
        repository.focusStats.onEach { stats ->
            _uiState.update { it.copy(stats = stats) }
        }.launchIn(viewModelScope)

        // Observe sensor test status
        repository.isSensorTestCompleted.onEach { completed ->
            _uiState.update { it.copy(isSensorTestCompleted = completed) }
        }.launchIn(viewModelScope)

        // Observe sensor fusion
        var hasVibratedForCalibration = false
        sensorManager.isFocusPosition.onEach { isFocusPosition ->
            if (!_uiState.value.isSensorTestCompleted) {
                if (isFocusPosition && !hasVibratedForCalibration) {
                    hasVibratedForCalibration = true
                    vibrateSuccess()
                } else if (!isFocusPosition) {
                    hasVibratedForCalibration = false
                }
            }
            handleSensorUpdate(isFocusPosition)
        }.launchIn(viewModelScope)
    }

    private fun handleSensorUpdate(isLockedPosition: Boolean) {
        val currentPhase = _uiState.value.phase
        _uiState.update { it.copy(isDeviceLocked = isLockedPosition) }

        if (!_uiState.value.isSensorTestCompleted) {
            return
        }

        when {
            // Device moved into focus position while waiting
            isLockedPosition && currentPhase == SessionPhase.WAITING -> {
                _uiState.update { it.copy(phase = SessionPhase.ACTIVE) }
                enableDnd()
                startTimer()
            }
            // Device moved into focus position during grace period — resume
            isLockedPosition && currentPhase == SessionPhase.BROKEN -> {
                stopAlarm()
                graceJob?.cancel()
                _uiState.update { it.copy(phase = SessionPhase.ACTIVE, graceCountdownSeconds = 10) }
                enableDnd()
                startTimer()
            }
            // Device lifted during active focus — trigger 10s grace period
            !isLockedPosition && currentPhase == SessionPhase.ACTIVE -> {
                timerJob?.cancel()
                _uiState.update { it.copy(phase = SessionPhase.BROKEN, graceCountdownSeconds = 10) }
                disableDnd()
                startAlarm()
                startGraceCountdown()
            }
        }
    }

    fun selectDuration(minutes: Int) {
        if (_uiState.value.phase == SessionPhase.IDLE) {
            _uiState.update {
                it.copy(
                    selectedDurationMinutes = minutes,
                    remainingSeconds = minutes * 60
                )
            }
        }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun startSession() {
        _uiState.update {
            it.copy(
                phase = SessionPhase.WAITING,
                remainingSeconds = it.selectedDurationMinutes * 60,
                graceCountdownSeconds = 10,
                isDeviceLocked = false
            )
        }
        sensorManager.start()
    }

    fun cancelSession() {
        timerJob?.cancel()
        graceJob?.cancel()
        stopAlarm()
        sensorManager.stop()
        disableDnd()
        _uiState.update { it.copy(phase = SessionPhase.IDLE) }
    }

    fun resetToIdle() {
        timerJob?.cancel()
        graceJob?.cancel()
        stopAlarm()
        sensorManager.stop()
        disableDnd()
        val currentDuration = _uiState.value.selectedDurationMinutes
        _uiState.update {
            FocusUiState(
                selectedDurationMinutes = currentDuration,
                remainingSeconds = currentDuration * 60,
                isMuted = it.isMuted,
                stats = it.stats,
                isSensorTestCompleted = it.isSensorTestCompleted
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1000L)
                _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }
            // Timer complete → success
            onSessionCompleted()
        }
    }

    private fun startGraceCountdown() {
        graceJob?.cancel()
        graceJob = viewModelScope.launch {
            repeat(10) { tick ->
                delay(1000L)
                val remaining = 9 - tick
                _uiState.update { it.copy(graceCountdownSeconds = remaining) }
                if (remaining <= 0) {
                    onSessionFailed()
                }
            }
        }
    }

    private fun onSessionCompleted() {
        val durationMins = _uiState.value.selectedDurationMinutes.toLong()
        stopAlarm()
        sensorManager.stop()
        disableDnd()
        _uiState.update { it.copy(phase = SessionPhase.COMPLETED) }
        viewModelScope.launch {
            repository.recordSuccess(durationMins)
        }
    }

    private fun onSessionFailed() {
        val elapsed = _uiState.value.selectedDurationMinutes * 60 - _uiState.value.remainingSeconds
        val elapsedMins = (elapsed / 60).toLong().coerceAtLeast(1L)
        stopAlarm()
        sensorManager.stop()
        disableDnd()
        _uiState.update { it.copy(phase = SessionPhase.FAILED) }
        viewModelScope.launch {
            repository.recordFailure(elapsedMins)
        }
    }

    private fun startAlarm() {
        val isMuted = _uiState.value.isMuted

        // Always vibrate — repeating alarm pattern
        try {
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Play ringtone only when not muted
        if (!isMuted) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ringtone = RingtoneManager.getRingtone(getApplication(), uri)
                ringtone?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopAlarm() {
        try {
            vibrator.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ringtone = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        graceJob?.cancel()
        stopAlarm()
        sensorManager.stop()
        disableDnd()
    }
}
