package com.mogalab.fliplock.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mogalab.fliplock.ui.theme.ChampagneGold
import com.mogalab.fliplock.ui.theme.FocusBlack
import com.mogalab.fliplock.ui.theme.MutedCoral
import com.mogalab.fliplock.ui.theme.ObsidianBlack
import com.mogalab.fliplock.ui.theme.PanelDark
import com.mogalab.fliplock.ui.theme.PanelSurface
import com.mogalab.fliplock.ui.theme.PulseGold
import com.mogalab.fliplock.ui.theme.TextPrimary
import com.mogalab.fliplock.ui.theme.TextSecondary
import com.mogalab.fliplock.ui.theme.TextTertiary
import com.mogalab.fliplock.ui.theme.WireframeLine

private val DURATION_PRESETS = listOf(10, 25, 45, 60)

// ═════════════════════════════════════════════════════════
// ROOT SCREEN SWITCHER
// ═════════════════════════════════════════════════════════
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
        targetValue   = if (uiState.phase == SessionPhase.ACTIVE) FocusBlack else ObsidianBlack,
        animationSpec = tween(600),
        label         = "bg_color"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        when (uiState.phase) {
            SessionPhase.IDLE, SessionPhase.WAITING ->
                IdleOrWaitingContent(
                    uiState            = uiState,
                    onDurationSelected = onDurationSelected,
                    onStartSession     = onStartSession,
                    onToggleMute       = onToggleMute
                )
            SessionPhase.ACTIVE ->
                ActiveFocusContent(uiState = uiState, onCancel = onCancelSession)
            SessionPhase.BROKEN ->
                BrokenSessionContent(uiState = uiState, onCancel = onCancelSession)
            SessionPhase.COMPLETED ->
                CompletionContent(uiState = uiState, onReset = onReset)
            SessionPhase.FAILED ->
                FailureContent(uiState = uiState, onReset = onReset)
        }
    }
}

// ═════════════════════════════════════════════════════════
// IDLE / WAITING  — main redesign
// ═════════════════════════════════════════════════════════
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
            .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Top bar: wireframe status label + mute square
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            WireframeStatusLabel(
                text = if (isWaiting) "FLIP DEVICE FACE-DOWN" else "READY TO FOCUS"
            )
            MuteSquareButton(isMuted = uiState.isMuted, onToggle = onToggleMute)
        }

        Spacer(Modifier.weight(1f))

        // ── Concentric-rings orb + ultra-thin clock
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier.size(280.dp)
        ) {
            ConcentricRings(modifier = Modifier.fillMaxSize())
            Text(
                text          = uiState.formattedTime,
                fontSize      = 72.sp,
                fontWeight    = FontWeight.Thin,
                color         = TextPrimary,
                letterSpacing = (-1).sp
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text          = "Focus by Gravity",
            fontSize      = 11.sp,
            color         = TextSecondary.copy(alpha = 0.55f),
            letterSpacing = 3.sp,
            fontWeight    = FontWeight.Light
        )

        Spacer(Modifier.weight(1f))

        // ── Control panel card
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = PanelDark),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier              = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment   = Alignment.CenterHorizontally
            ) {
                if (!isWaiting) {
                    SectionLabel("SET DURATION")
                    Spacer(Modifier.height(10.dp))

                    var durationText by remember {
                        mutableStateOf(uiState.selectedDurationMinutes.toString())
                    }
                    LaunchedEffect(uiState.selectedDurationMinutes) {
                        durationText = uiState.selectedDurationMinutes.toString()
                    }

                    DurationInputBox(
                        durationText  = durationText,
                        onValueChange = { raw ->
                            if (raw.length <= 3) {
                                val digits = raw.filter { it.isDigit() }
                                durationText = digits
                                digits.toIntOrNull()?.let { mins ->
                                    if (mins in 1..999) onDurationSelected(mins)
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(14.dp))
                    SectionLabel("SUGGESTIONS", dimmed = true)
                    Spacer(Modifier.height(8.dp))

                    SegmentedSuggestionsBar(
                        presets         = DURATION_PRESETS,
                        selectedMinutes = uiState.selectedDurationMinutes,
                        onSelect        = { mins ->
                            durationText = mins.toString()
                            onDurationSelected(mins)
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                } else {
                    Text(
                        text          = "Place your phone screen-down on a flat surface to begin.",
                        fontSize      = 13.sp,
                        color         = TextSecondary,
                        textAlign     = TextAlign.Center,
                        letterSpacing = 0.2.sp
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Wireframe hollow CTA button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            if (isWaiting) WireframeLine else ChampagneGold,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { if (!isWaiting) onStartSession() }
                ) {
                    Text(
                        text          = if (isWaiting) "WAITING FOR FLIP…" else "START LOCK",
                        fontSize      = 13.sp,
                        fontWeight    = FontWeight.Medium,
                        color         = if (isWaiting) TextSecondary else ChampagneGold,
                        letterSpacing = 3.sp
                    )
                }

                Spacer(Modifier.height(22.dp))
                StatsRow(uiState = uiState)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════
// ACTIVE — OLED pitch-black with breathing gold pulse
// ═════════════════════════════════════════════════════════
@Composable
private fun ActiveFocusContent(uiState: FocusUiState, onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 0.80f,
        targetValue   = 1.18f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.06f,
        targetValue   = 0.20f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "alpha"
    )

    Box(
        modifier         = Modifier.fillMaxSize().background(FocusBlack),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .alpha(pulseAlpha)
                .background(Brush.radialGradient(listOf(PulseGold, Color.Transparent)))
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = uiState.formattedTime,
                fontSize   = 56.sp,
                fontWeight = FontWeight.Thin,
                color      = TextPrimary.copy(alpha = 0.14f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text          = "● FOCUSING",
                fontSize      = 10.sp,
                color         = ChampagneGold.copy(alpha = 0.28f),
                letterSpacing = 3.sp
            )
        }
        Text(
            text          = "TAP TO CANCEL",
            fontSize      = 10.sp,
            color         = TextTertiary.copy(alpha = 0.5f),
            letterSpacing = 1.sp,
            modifier      = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp)
                .clickable { onCancel() }
        )
    }
}

// ═════════════════════════════════════════════════════════
// BROKEN — 10-second grace period, coral flash overlay
// ═════════════════════════════════════════════════════════
@Composable
private fun BrokenSessionContent(uiState: FocusUiState, onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.10f,
        targetValue   = 0.42f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label         = "flash"
    )

    Box(
        modifier         = Modifier.fillMaxSize().background(ObsidianBlack),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(flashAlpha)
                .background(MutedCoral.copy(alpha = 0.18f))
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 32.dp)
        ) {
            Text("⚠", fontSize = 60.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                text          = "SESSION BROKEN",
                fontSize      = 17.sp,
                color         = MutedCoral,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text      = "Flip device face-down to resume",
                fontSize  = 13.sp,
                color     = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(130.dp)
                    .border(1.dp, MutedCoral.copy(alpha = 0.65f), CircleShape)
            ) {
                Text(
                    text       = "${uiState.graceCountdownSeconds}",
                    fontSize   = 64.sp,
                    fontWeight = FontWeight.Thin,
                    color      = MutedCoral
                )
            }
            Spacer(Modifier.height(40.dp))
            Text(
                text          = "TAP TO CANCEL",
                fontSize      = 10.sp,
                color         = TextTertiary,
                letterSpacing = 1.5.sp,
                modifier      = Modifier.clickable { onCancel() }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════
// COMPLETION
// ═════════════════════════════════════════════════════════
@Composable
private fun CompletionContent(uiState: FocusUiState, onReset: () -> Unit) {
    Box(
        modifier         = Modifier.fillMaxSize().background(ObsidianBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Text("✦", fontSize = 52.sp, color = ChampagneGold)
            Spacer(Modifier.height(16.dp))
            Text(
                text          = "SESSION COMPLETE",
                fontSize      = 16.sp,
                color         = ChampagneGold,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = "${uiState.selectedDurationMinutes} focused minutes logged.",
                fontSize  = 13.sp,
                color     = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = PanelDark)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItemV("${uiState.stats.successfulMinutes}m", "FOCUSED", ChampagneGold)
                    StatItemV(
                        "${uiState.stats.successCount}/${uiState.stats.sessionCount}",
                        "SESSIONS", TextPrimary
                    )
                }
            }
            Spacer(Modifier.height(36.dp))
            WireframeCtaButton("START NEW SESSION", ChampagneGold, onReset)
        }
    }
}

// ═════════════════════════════════════════════════════════
// FAILURE
// ═════════════════════════════════════════════════════════
@Composable
private fun FailureContent(uiState: FocusUiState, onReset: () -> Unit) {
    Box(
        modifier         = Modifier.fillMaxSize().background(ObsidianBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Text("✕", fontSize = 52.sp, color = MutedCoral)
            Spacer(Modifier.height(16.dp))
            Text(
                text          = "SESSION FAILED",
                fontSize      = 16.sp,
                color         = MutedCoral,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = "You lifted the device before the session ended.",
                fontSize  = 13.sp,
                color     = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = PanelDark)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItemV("${uiState.stats.failedMinutes}m", "FAILED", MutedCoral)
                    StatItemV(
                        "${uiState.stats.successCount}/${uiState.stats.sessionCount}",
                        "SESSIONS", TextPrimary
                    )
                }
            }
            Spacer(Modifier.height(36.dp))
            WireframeCtaButton("TRY AGAIN", MutedCoral, onReset)
        }
    }
}

// ═════════════════════════════════════════════════════════
// PRIVATE COMPONENTS
// ═════════════════════════════════════════════════════════

/** Razor-thin rectangular wireframe status label (no round corners) */
@Composable
private fun WireframeStatusLabel(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, TextTertiary, RoundedCornerShape(3.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text          = text,
            fontSize      = 11.sp,
            color         = TextSecondary,
            fontWeight    = FontWeight.Light,
            letterSpacing = 1.5.sp
        )
    }
}

/** Thin-lined square mute toggle — coral border when muted */
@Composable
private fun MuteSquareButton(isMuted: Boolean, onToggle: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier
            .size(40.dp)
            .border(
                1.dp,
                if (isMuted) MutedCoral.copy(alpha = 0.55f) else TextTertiary,
                RoundedCornerShape(8.dp)
            )
            .clickable { onToggle() }
    ) {
        Text(
            text     = if (isMuted) "🔕" else "🔔",
            fontSize = 16.sp
        )
    }
}

/** Ethereal concentric wireframe rings drawn on Canvas */
@Composable
private fun ConcentricRings(modifier: Modifier = Modifier) {
    val lineColor = Color(0xFF2E2E2E)
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR   = size.minDimension / 2f
        listOf(0.97f, 0.82f, 0.68f, 0.55f, 0.43f, 0.32f).forEachIndexed { i, frac ->
            drawCircle(
                color  = lineColor.copy(alpha = (0.55f - i * 0.08f).coerceAtLeast(0.08f)),
                radius = maxR * frac,
                center = center,
                style  = Stroke(width = 1.1.dp.toPx())
            )
        }
    }
}

/** Integrated dark input box — label top-left, large number center, "min" trailing */
@Composable
private fun DurationInputBox(durationText: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value           = durationText,
        onValueChange   = onValueChange,
        label           = { Text("Duration", fontSize = 11.sp) },
        trailingIcon    = {
            Text(
                text     = "min",
                fontSize = 14.sp,
                color    = TextSecondary,
                modifier = Modifier.padding(end = 12.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine      = true,
        shape           = RoundedCornerShape(12.dp),
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = ChampagneGold.copy(alpha = 0.45f),
            unfocusedBorderColor    = WireframeLine,
            focusedLabelColor       = ChampagneGold,
            unfocusedLabelColor     = TextSecondary,
            cursorColor             = ChampagneGold,
            focusedTextColor        = TextPrimary,
            unfocusedTextColor      = TextPrimary,
            unfocusedContainerColor = PanelSurface,
            focusedContainerColor   = PanelSurface
        ),
        textStyle       = MaterialTheme.typography.displayMedium.copy(
            fontSize   = 36.sp,
            fontWeight = FontWeight.Light,
            textAlign  = TextAlign.Center
        ),
        modifier        = Modifier.fillMaxWidth()
    )
}

/** Horizontal segmented bar: faint dividers + champagne-gold rounded pill for selected */
@Composable
private fun SegmentedSuggestionsBar(
    presets: List<Int>,
    selectedMinutes: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, WireframeLine, RoundedCornerShape(12.dp))
            .background(PanelSurface)
    ) {
        presets.forEachIndexed { index, mins ->
            val isSelected = selectedMinutes == mins
            // Faint vertical divider between segments
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(WireframeLine)
                )
            }
            // Segment slot
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(mins) }
            ) {
                // Gold pill (selected only)
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(5.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ChampagneGold)
                    )
                }
                Text(
                    text       = "${mins}M",
                    fontSize   = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isSelected) ObsidianBlack else TextSecondary
                )
            }
        }
    }
}

/** Tiny muted uppercase section header */
@Composable
private fun SectionLabel(text: String, dimmed: Boolean = false) {
    Text(
        text          = text,
        fontSize      = 10.sp,
        color         = if (dimmed) TextTertiary else TextSecondary,
        letterSpacing = 2.sp,
        fontWeight    = FontWeight.Normal
    )
}

/** Bottom stats row: focused / sessions / failed (with ✦ accent) */
@Composable
private fun StatsRow(uiState: FocusUiState) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItemV("${uiState.stats.successfulMinutes}m", "FOCUSED", TextPrimary)
        StatItemV("${uiState.stats.sessionCount}", "SESSIONS", TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatItemV("${uiState.stats.failedMinutes}m", "FAILED", MutedCoral)
            if (uiState.stats.failedMinutes > 0) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text     = "✦",
                    color    = MutedCoral,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .align(Alignment.Top)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

/** Stacked stat: large value + microscopic label */
@Composable
private fun StatItemV(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Normal,
            color      = color
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text          = label,
            fontSize      = 9.sp,
            color         = TextSecondary,
            letterSpacing = 1.sp
        )
    }
}

/** Hollow wireframe button used on completion/failure screens */
@Composable
private fun WireframeCtaButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, color, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Text(
            text          = label,
            fontSize      = 13.sp,
            fontWeight    = FontWeight.Medium,
            color         = color,
            letterSpacing = 2.5.sp
        )
    }
}
