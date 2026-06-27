package com.mogalab.fliplock.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Backgrounds — matte obsidian black ─────────────────
val ObsidianBlack    = Color(0xFF090909)   // primary screen background
val PanelDark        = Color(0xFF141414)   // card / control panel
val PanelSurface     = Color(0xFF1C1C1C)   // elevated input surfaces
val WireframeLine    = Color(0xFF2A2A2A)   // thin borders / ring strokes / dividers

// ─── Primary accent — champagne gold ────────────────────
val ChampagneGold    = Color(0xFFD4A86A)   // selected pill, CTA border, gold text
val ChampagneGoldDim = Color(0xFF8A6C42)   // dimmed gold for waiting state

// ─── Negative accent — desaturated coral ────────────────
val MutedCoral       = Color(0xFFBF7060)   // failed, muted-bell state, penalty screen
val MutedCoralDim    = Color(0xFF8A4840)   // dimmed coral

// ─── Typography ──────────────────────────────────────────
val TextPrimary      = Color(0xFFF2F2EC)   // near-white — clock, numbers
val TextSecondary    = Color(0xFF6A6A6A)   // muted — labels, sub-text
val TextTertiary     = Color(0xFF363636)   // very muted — wireframe borders, section hints

// ─── Focus-mode specifics ────────────────────────────────
val FocusBlack       = Color(0xFF000000)   // OLED pitch-black when face-down
val PulseGold        = Color(0xFFD4A86A)   // breathing glow color (active mode)