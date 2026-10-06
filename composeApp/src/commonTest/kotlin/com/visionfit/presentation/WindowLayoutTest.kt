package com.visionfit.presentation

import androidx.compose.ui.unit.dp
import com.visionfit.presentation.designsystem.layout.HeightClass
import com.visionfit.presentation.designsystem.layout.WidthClass
import com.visionfit.presentation.designsystem.layout.WindowLayout
import com.visionfit.presentation.designsystem.layout.centeringPadding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowLayoutTest {

    @Test
    fun designPhoneKeepsThePhoneLayout() {
        val phone = WindowLayout.Phone
        assertEquals(WidthClass.COMPACT, phone.widthClass)
        assertEquals(HeightClass.MEDIUM, phone.heightClass)
        assertFalse(phone.isNarrow)
        assertFalse(phone.usesNavigationRail)
        assertFalse(phone.usesTwoPanes)
        assertEquals(20.dp, phone.gutter)
    }

    @Test
    fun smallPhoneTightensTheGutter() {
        val small = WindowLayout(320.dp, 568.dp)
        assertTrue(small.isNarrow)
        assertFalse(small.usesNavigationRail)
        assertEquals(16.dp, small.gutter)
    }

    @Test
    fun phonesHeldSidewaysGetTheRailAndTwoPanes() {
        listOf(WindowLayout(844.dp, 390.dp), WindowLayout(740.dp, 360.dp), WindowLayout(568.dp, 320.dp)).forEach { landscape ->
            assertTrue(landscape.isShort, "$landscape is short")
            assertTrue(landscape.usesNavigationRail, "$landscape uses the rail")
            assertTrue(landscape.usesTwoPanes, "$landscape uses two panes")
            assertEquals(20.dp, landscape.gutter)
        }
    }

    @Test
    fun portraitTabletUsesTheRailWithOneCenteredColumn() {
        val tablet = WindowLayout(834.dp, 1194.dp)
        assertEquals(WidthClass.MEDIUM, tablet.widthClass)
        assertEquals(HeightClass.EXPANDED, tablet.heightClass)
        assertTrue(tablet.usesNavigationRail)
        assertFalse(tablet.usesTwoPanes)
        assertEquals(28.dp, tablet.gutter)
    }

    @Test
    fun desktopAndLandscapeTabletUseTwoPanes() {
        listOf(WindowLayout(1280.dp, 800.dp), WindowLayout(1194.dp, 834.dp)).forEach { wide ->
            assertEquals(WidthClass.EXPANDED, wide.widthClass)
            assertTrue(wide.usesTwoPanes)
            assertEquals(32.dp, wide.gutter)
        }
    }

    @Test
    fun centeringPaddingNeverGoesBelowTheGutter() {
        assertEquals(20.dp, centeringPadding(containerWidth = 390.dp, maxContentWidth = 640.dp, minPadding = 20.dp))
        assertEquals(97.dp, centeringPadding(containerWidth = 834.dp, maxContentWidth = 640.dp, minPadding = 28.dp))
    }
}
