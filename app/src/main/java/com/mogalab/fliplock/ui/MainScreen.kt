package com.mogalab.fliplock.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mogalab.fliplock.ui.theme.BackgroundCard
import com.mogalab.fliplock.ui.theme.BackgroundDeep
import com.mogalab.fliplock.ui.theme.BackgroundElevated
import com.mogalab.fliplock.ui.theme.DangerRed
import com.mogalab.fliplock.ui.theme.DangerRedAlpha
import com.mogalab.fliplock.ui.theme.FocusBlack
import com.mogalab.fliplock.ui.theme.NeonGreen
import com.mogalab.fliplock.ui.theme.NeonGreenAlpha
import com.mogalab.fliplock.ui.theme.NeonGreenDim
import com.mogalab.fliplock.ui.theme.PulseGreen
import com.mogalab.fliplock.ui.theme.SuccessGreen
import com.mogalab.fliplock.ui.theme.TextMuted
import com.mogalab.fliplock.ui.theme.TextPrimary
import com.mogalab.fliplock.ui.theme.TextSecondary

private val DURATION_PRESETS = listOf(10, 25, 45, 60)

@Composable
fun MainScreen(
    uiState: FocusUiState,
    onDurationSelected: (Int) -> Unit,
    onStartSession: () -> Unit,
    onCancelSession: () -> Unit,
    onReset: () -> Unit,
    onToggleMute: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = when (uiState.phase) {
            SessionPhase.ACTIVE  -> FocusBlack
            SessionPhase.BROKEN  -> BackgroundDeep
            else                 -> BackgroundDeep
        },
        animationSpec = tween(600),
        label = "bg_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        when (uiState.phase) {
            SessionPhase.IDLE, SessionPhase.WAITING -> {
                IdleOrWaitingContent(
                    uiState = uiState,
                    onDurationSelected = onDurationSelected,
                    onStartSession = onStartSession,
                    onToggleMute = onToggleMute
                )
            }
            SessionPhase.ACTIVE -> {
                ActiveFocusContent(uiState = uiState, onCancel = onCancelSession)
            }
            SessionPhase.BROKEN -> {
                BrokenSessionContent(uiState = uiState, onCancel = onCancelSession)
            }
            SessionPhase.COMPLETED -> {
                CompletionContent(uiState = uiState, onReset = onReset)
            }
            SessionPhase.FAILED -> {
                FailureContent(uiState = uiState, onReset = onReset)
            }
        }
    }
}

// ─────────────────────────────────────────
// IDLE / WAITING SCREEN
// ─────────────────────────────────────────
@Composable
private fun IdleOrWaitingContent(
    uiState: FocusUiState,
    onDurationSelected: (Int) -> Unit,
    onStartSession: () -> Unit,
    onToggleMute: () -> Unit
) {
    val isWaiting = uiState.phase == SessionPhase.WAITING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Top bar: status chip + mute toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(
                text = if (isWaiting) "FLIP DEVICE FACE-DOWN" else "READY TO FOCUS",
                color = if (isWaiting) NeonGreen else TextSecondary
            )
            MuteToggleButton(isMuted = uiState.isMuted, onToggle = onToggleMute)
        }

        Spacer(Modifier.weight(1f))

        // ── Giant timer display
        Box(contentAlignment = Alignment.Center) {
            // Glow ring behind clock
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(NeonGreenAlpha)
            )
            Text(
                text = uiState.formattedTime,
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Focus by Gravity",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.weight(1f))

        // ── Bottom card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isWaiting) {
                    Text(
                        text = "SET DURATION",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    // ── Custom duration text field
                    var durationText by remember { mutableStateOf(uiState.selectedDurationMinutes.toString()) }
                    LaunchedEffect(uiState.selectedDurationMinutes) {
                        durationText = uiState.selectedDurationMinutes.toString()
                    }
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { text ->
                            // Allow empty while typing, but only commit valid values
                            if (text.length <= 4) {
                                durationText = text.filter { it.isDigit() }
                                durationText.toIntOrNull()?.let { mins ->
                                    if (mins in 1..999) onDurationSelected(mins)
                                }
                            }
                        },
                        label = { Text("Duration") },
                        trailingIcon = {
                            Text(
                                "min",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
                            focusedLabelColor = NeonGreen,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = NeonGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            unfocusedContainerColor = BackgroundElevated,
                            focusedContainerColor = BackgroundElevated
                        ),
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 32.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "SUGGESTIONS",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DURATION_PRESETS.forEach { mins ->
                            DurationChip(
                                minutes = mins,
                                isSelected = uiState.selectedDurationMinutes == mins,
                                onClick = {
                                    durationText = mins.toString()
                                    onDurationSelected(mins)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                } else {
                    Text(
                        text = "Place your phone screen-down on a flat surface to begin.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                }

                // Start / Cancel button
                Button(
                    onClick = { if (!isWaiting) onStartSession() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWaiting) NeonGreenDim else NeonGreen,
                        contentColor   = BackgroundDeep
                    )
                ) {
                    Text(
                        text = if (isWaiting) "⏳  WAITING FOR FLIP…" else "START LOCK",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }

                // Stats strip
                Spacer(Modifier.height(20.dp))
                StatsStrip(uiState = uiState)
            }
        }
    }
}

// ─────────────────────────────────────────
// ACTIVE FOCUS SCREEN (OLED BLACK)
// ─────────────────────────────────────────
@Composable
private fun ActiveFocusContent(uiState: FocusUiState, onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue  = 0.45f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusBlack),
        contentAlignment = Alignment.Center
    ) {
        // Breathing circle
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .alpha(pulseAlpha)
                .background(
                    Brush.radialGradient(
                        listOf(PulseGreen, Color.Transparent)
                    )
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = uiState.formattedTime,
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary.copy(alpha = 0.18f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "● FOCUSING",
                style = MaterialTheme.typography.bodySmall,
                color = PulseGreen.copy(alpha = 0.4f),
                letterSpacing = 3.sp
            )
        }

        // Tap-to-cancel — bottom hint
        Text(
            text = "TAP TO CANCEL",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted.copy(alpha = 0.4f),
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .clickable { onCancel() }
        )
    }
}

// ─────────────────────────────────────────
// BROKEN / GRACE PERIOD SCREEN
// ─────────────────────────────────────────
@Composable
private fun BrokenSessionContent(uiState: FocusUiState, onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.7f,
        animationSpec = infiniteRepeatable(
            animation  = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flash_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep),
        contentAlignment = Alignment.Center
    ) {
        // Red flash overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(flashAlpha)
                .background(DangerRedAlpha)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "⚠",
                fontSize = 72.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "SESSION BROKEN!",
                style = MaterialTheme.typography.headlineMedium,
                color = DangerRed,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Flip device face-down to resume",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))

            // Grace countdown
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .border(4.dp, DangerRed, CircleShape)
            ) {
                Text(
                    text = "${uiState.graceCountdownSeconds}",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    color = DangerRed
                )
            }

            Spacer(Modifier.height(40.dp))
            Text(
                text = "TAP TO CANCEL SESSION",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.clickable { onCancel() }
            )
        }
    }
}

// ─────────────────────────────────────────
// COMPLETION SCREEN (SUCCESS)
// ─────────────────────────────────────────
@Composable
private fun CompletionContent(uiState: FocusUiState, onReset: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(BackgroundDeep, Color(0xFF0A2016)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "✅", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "SESSION COMPLETE",
                style = MaterialTheme.typography.headlineMedium,
                color = SuccessGreen,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${uiState.selectedDurationMinutes} focused minutes logged.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))
            StatsCard(uiState = uiState)
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor   = BackgroundDeep
                )
            ) {
                Text(
                    "START NEW SESSION",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────
// FAILURE SCREEN
// ─────────────────────────────────────────
@Composable
private fun FailureContent(uiState: FocusUiState, onReset: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(BackgroundDeep, Color(0xFF200A0A)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "❌", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "SESSION FAILED",
                style = MaterialTheme.typography.headlineMedium,
                color = DangerRed,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "You lifted the device before the session ended.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))
            StatsCard(uiState = uiState)
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor   = TextPrimary
                )
            ) {
                Text(
                    "TRY AGAIN",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────
// SHARED COMPONENTS
// ─────────────────────────────────────────

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun DurationChip(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) NeonGreen else BackgroundElevated)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) Color.Transparent else TextMuted.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = "${minutes}m",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) BackgroundDeep else TextSecondary
        )
    }
}

@Composable
private fun StatsStrip(uiState: FocusUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            label  = "FOCUSED",
            value  = "${uiState.stats.successfulMinutes}m",
            color  = NeonGreen
        )
        StatItem(
            label  = "SESSIONS",
            value  = "${uiState.stats.sessionCount}",
            color  = TextSecondary
        )
        StatItem(
            label  = "FAILED",
            value  = "${uiState.stats.failedMinutes}m",
            color  = DangerRed
        )
    }
}

@Composable
private fun StatsCard(uiState: FocusUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label  = "TOTAL FOCUSED",
                value  = "${uiState.stats.successfulMinutes}m",
                color  = NeonGreen
            )
            StatItem(
                label  = "SESSIONS",
                value  = "${uiState.stats.successCount}/${uiState.stats.sessionCount}",
                color  = TextSecondary
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun MuteToggleButton(isMuted: Boolean, onToggle: () -> Unit) {
    val bgColor   = if (isMuted) DangerRed.copy(alpha = 0.15f) else NeonGreenAlpha
    val border    = if (isMuted) DangerRed.copy(alpha = 0.45f) else NeonGreen.copy(alpha = 0.45f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, border, CircleShape)
            .clickable { onToggle() }
    ) {
        Text(
            text     = if (isMuted) "🔕" else "🔔",
            fontSize = 20.sp
        )
    }
}
