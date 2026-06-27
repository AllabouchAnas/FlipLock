package com.mogalab.fliplock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FlipLockColorScheme = darkColorScheme(
    primary          = ChampagneGold,
    onPrimary        = ObsidianBlack,
    secondary        = ChampagneGoldDim,
    onSecondary      = ObsidianBlack,
    background       = ObsidianBlack,
    onBackground     = TextPrimary,
    surface          = PanelDark,
    onSurface        = TextPrimary,
    surfaceVariant   = PanelSurface,
    onSurfaceVariant = TextSecondary,
    error            = MutedCoral,
    onError          = TextPrimary,
    outline          = WireframeLine
)

@Composable
fun FlipLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FlipLockColorScheme,
        typography  = FlipLockTypography,
        content     = content
    )
}