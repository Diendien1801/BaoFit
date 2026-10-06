package com.visionfit.presentation.designsystem.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material 3 window size classes by available width. */
enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

/** Window size classes by available height. [COMPACT] is a phone held sideways. */
enum class HeightClass { COMPACT, MEDIUM, EXPANDED }

/**
 * The space the app is drawn in and the layout decisions every screen derives from it.
 * Breakpoints follow the Material 3 window size classes: 600 / 840 dp wide, 480 / 900 dp tall.
 */
@Immutable
data class WindowLayout(val width: Dp, val height: Dp) {

    val widthClass: WidthClass = when {
        width < 600.dp -> WidthClass.COMPACT
        width < 840.dp -> WidthClass.MEDIUM
        else -> WidthClass.EXPANDED
    }

    val heightClass: HeightClass = when {
        height < 480.dp -> HeightClass.COMPACT
        height < 900.dp -> HeightClass.MEDIUM
        else -> HeightClass.EXPANDED
    }

    /** Narrower than the 390 dp design, such as an iPhone SE or a small Android phone. */
    val isNarrow: Boolean get() = width < 360.dp

    /** Little vertical room (a phone in landscape): headers and footers shrink or move aside. */
    val isShort: Boolean get() = heightClass == HeightClass.COMPACT

    /** The main tabs swap the floating bottom bar for a side rail. */
    val usesNavigationRail: Boolean get() = widthClass != WidthClass.COMPACT || isShort

    /** Screens put their two halves (photo | details, summary | list) side by side. */
    val usesTwoPanes: Boolean get() = widthClass == WidthClass.EXPANDED || (isShort && width >= 560.dp)

    /** Space between content and the window edge. */
    val gutter: Dp
        get() = when {
            isNarrow -> 16.dp
            widthClass == WidthClass.COMPACT || isShort -> 20.dp
            widthClass == WidthClass.MEDIUM -> 28.dp
            else -> 32.dp
        }

    companion object {
        /** The 390 × 844 dp phone the design was drawn for. */
        val Phone = WindowLayout(width = 390.dp, height = 844.dp)
    }
}

val LocalWindowLayout = staticCompositionLocalOf { WindowLayout.Phone }

/** Measures the space given to the app and provides it as [LocalWindowLayout]. */
@Composable
fun ProvideWindowLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val layout = remember(maxWidth, maxHeight) { WindowLayout(maxWidth, maxHeight) }
        CompositionLocalProvider(LocalWindowLayout provides layout, content = content)
    }
}

/** System bars and display cutout, without the keyboard. */
val WindowInsets.Companion.safeArea: WindowInsets
    @Composable get() = systemBars.union(displayCutout)

/** The left and right safe area: the cutout and rounded corners of a phone held sideways. */
val WindowInsets.Companion.safeHorizontal: WindowInsets
    @Composable get() = safeArea.only(WindowInsetsSides.Horizontal)
