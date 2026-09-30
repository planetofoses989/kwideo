package com.planetofoses989.kwideo.ui.utils

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import com.planetofoses989.kwideo.presentation.Screen

val LocalBackStack: ProvidableCompositionLocal<NavBackStack<Screen>> =
  compositionLocalOf { error("LocalBackStack not initialized!") }

/**
 * Optional back handler for detail screens embedded in a two-pane layout.
 * When provided (tablet master-detail), detail back buttons invoke this
 * instead of popping the real nav stack or finishing the activity.
 */
val LocalDetailPaneBack: ProvidableCompositionLocal<(() -> Unit)?> =
  compositionLocalOf { null }
