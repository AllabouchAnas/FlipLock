package com.mogalab.fliplock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FlipLockColorScheme = darkColorScheme(
    primary         = NeonGreen,
    onPrimary       = BackgroundDeep,
    secondary       = NeonGreenDim,
    onSecondary     = BackgroundDeep,
    background      = BackgroundDeep,
    onBackground    = TextPrimary,
    surface         = BackgroundCard,
    onSurface       = TextPrimary,
    surfaceVariant  = BackgroundElevated,
    onSurfaceVariant = TextSecondary,
    error           = DangerRed,
    onError         = TextPrimary,
    outline         = SurfaceOverlay
)

@Composable
fun FlipLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FlipLockColorScheme,
        typography  = FlipLockTypography,
        content     = content
    )
}