package it.orma.cammino.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** Se true, le animazioni non essenziali vanno ridotte (accessibilità). */
val LocalReduceAnimations = compositionLocalOf { false }
