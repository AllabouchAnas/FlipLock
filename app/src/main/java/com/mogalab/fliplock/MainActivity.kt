package com.mogalab.fliplock

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mogalab.fliplock.ui.FocusViewModel
import com.mogalab.fliplock.ui.MainScreen
import com.mogalab.fliplock.ui.SessionPhase
import com.mogalab.fliplock.ui.theme.FlipLockTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FlipLockTheme {
                val viewModel: FocusViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // Keep screen on when a session is active so sensors stay alive
                val keepScreenOn = uiState.phase != SessionPhase.IDLE
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }

                MainScreen(
                    uiState            = uiState,
                    onDurationSelected = viewModel::selectDuration,
                    onStartSession     = viewModel::startSession,
                    onCancelSession    = viewModel::cancelSession,
                    onReset            = viewModel::resetToIdle
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Keep sensors registered — ViewModel lifecycle handles them
    }
}