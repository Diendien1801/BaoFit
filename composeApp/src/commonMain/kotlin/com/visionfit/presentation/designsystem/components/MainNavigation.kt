package com.visionfit.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.visionfit.presentation.designsystem.icons.IconSpec
import com.visionfit.presentation.designsystem.icons.VfIcon
import com.visionfit.presentation.designsystem.icons.VfIcons
import com.visionfit.presentation.designsystem.layout.LocalWindowLayout
import com.visionfit.presentation.designsystem.layout.safeArea
import com.visionfit.presentation.designsystem.theme.VfDimens
import com.visionfit.presentation.designsystem.theme.VisionFitTheme

enum class MainTab(val label: String, val icon: IconSpec) {
    TODAY("Hôm nay", VfIcons.Home),
    HISTORY("Lịch sử", VfIcons.Clock),
    GOAL("Mục tiêu", VfIcons.Target),
    PROFILE("Hồ sơ", VfIcons.User),
}

/**
 * Frame for the main tabs. Phones get the floating bottom bar; tablets, desktops and phones
 * held sideways get a side rail instead, so the bar never covers the little height they have.
 * [content] receives the padding that keeps it clear of the navigation and the system bars;
 * horizontal gutters are left to the screen.
 */
@Composable
fun MainTabScaffold(
    selected: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val safe = WindowInsets.safeArea.asPaddingValues()
    if (LocalWindowLayout.current.usesNavigationRail) {
        Row(modifier.fillMaxSize()) {
            VfNavigationRail(selected = selected, onTabSelected = onTabSelected, onCaptureClick = onCaptureClick)
            Box(Modifier.weight(1f).fillMaxHeight()) {
                content(
                    PaddingValues(
                        end = safe.calculateEndPadding(layoutDirection),
                        top = safe.calculateTopPadding(),
                        bottom = safe.calculateBottomPadding() + 24.dp,
                    ),
                )
            }
        }
    } else {
        Box(modifier.fillMaxSize()) {
            content(
                PaddingValues(
                    start = safe.calculateStartPadding(layoutDirection),
                    end = safe.calculateEndPadding(layoutDirection),
                    top = safe.calculateTopPadding(),
                    bottom = safe.calculateBottomPadding() + VfDimens.BottomBarClearance,
                ),
            )
            VfBottomNavBar(
                selected = selected,
                onTabSelected = onTabSelected,
                onCaptureClick = onCaptureClick,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * Floating navigation card with the raised camera button in the middle. Place it at the
 * bottom of a Box; it adds the navigation-bar inset itself.
 */
@Composable
fun VfBottomNavBar(
    selected: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
    ) {
        BrutalSurface(
            modifier = Modifier.fillMaxWidth().height(70.dp),
            shape = RoundedCornerShape(26.dp),
            shadowOffset = VfDimens.ShadowM,
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavItem(MainTab.TODAY, selected, onTabSelected, Modifier.weight(1f))
                NavItem(MainTab.HISTORY, selected, onTabSelected, Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
                NavItem(MainTab.GOAL, selected, onTabSelected, Modifier.weight(1f))
                NavItem(MainTab.PROFILE, selected, onTabSelected, Modifier.weight(1f))
            }
        }
        CaptureButton(
            onClick = onCaptureClick,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-14).dp),
        )
    }
}

/**
 * Vertical counterpart of [VfBottomNavBar]: the camera button above a card of tabs, centered
 * on the left edge. Scrolls when the window is too short for all of it.
 */
@Composable
fun VfNavigationRail(
    selected: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeArea.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .padding(start = 16.dp, end = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            // Room above the button for its ping, which the scroll container would clip.
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CaptureButton(onClick = onCaptureClick)
            BrutalSurface(
                modifier = Modifier.width(RailWidth),
                shape = RoundedCornerShape(26.dp),
                shadowOffset = VfDimens.ShadowM,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    MainTab.entries.forEach { tab ->
                        NavItem(tab, selected, onTabSelected, Modifier.fillMaxWidth().height(60.dp))
                    }
                }
            }
        }
    }
}

private val RailWidth = 84.dp

@Composable
private fun NavItem(tab: MainTab, selected: MainTab, onTabSelected: (MainTab) -> Unit, modifier: Modifier) {
    val isSelected = tab == selected
    val color = if (isSelected) VisionFitTheme.colors.primary else VisionFitTheme.colors.textSecondary
    Column(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(16.dp))
            .semantics { this.selected = isSelected }
            .clickable(role = Role.Tab, onClick = { onTabSelected(tab) }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        VfIcon(
            icon = tab.icon,
            contentDescription = null,
            size = 22.dp,
            tint = color,
            fill = if (isSelected) VisionFitTheme.colors.primaryContainer else Color.Transparent,
            strokeWidth = if (isSelected) 2.2f else 2f,
        )
        Text(
            text = tab.label,
            style = VisionFitTheme.type.chip.copy(fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold),
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun CaptureButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(66.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(66.dp)
                .pinging()
                .clip(CircleShape)
                .background(VisionFitTheme.colors.coral),
        )
        BrutalSurface(
            modifier = Modifier.size(66.dp),
            shape = CircleShape,
            color = VisionFitTheme.colors.coral,
            shadowOffset = VfDimens.ShadowM,
            onClick = onClick,
            onClickLabel = "Chụp ảnh bữa ăn",
            contentAlignment = Alignment.Center,
        ) {
            VfIcon(
                icon = VfIcons.Camera,
                contentDescription = "Chụp ảnh bữa ăn",
                size = 28.dp,
                tint = VisionFitTheme.colors.ink,
                fill = VisionFitTheme.colors.surface,
                accent = VisionFitTheme.colors.yellow,
            )
        }
    }
}
