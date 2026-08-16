package app.usenekko.adaptive

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptivePresentationPolicyTest {
    @Test
    fun contactUsesSinglePaneBelowExpandedWidth() {
        assertEquals(AdaptivePresentation.SinglePane, contactPresentation(windowWidthSizeClass(599.dp)))
        assertEquals(AdaptivePresentation.SinglePane, contactPresentation(windowWidthSizeClass(839.dp)))
    }

    @Test
    fun contactUsesSupportingPaneAtExpandedWidth() {
        assertEquals(AdaptivePresentation.SupportingPane, contactPresentation(windowWidthSizeClass(840.dp)))
    }

    @Test
    fun groupRequiresStableMasterDetailContext() {
        assertEquals(
            AdaptivePresentation.SinglePane,
            groupPresentation(WindowWidthSizeClass.Expanded, hasStableMasterDetailContext = false),
        )
        assertEquals(
            AdaptivePresentation.SupportingPane,
            groupPresentation(WindowWidthSizeClass.Expanded, hasStableMasterDetailContext = true),
        )
    }

    @Test
    fun groupRemainsSinglePaneBelowExpandedWidth() {
        assertEquals(
            AdaptivePresentation.SinglePane,
            groupPresentation(WindowWidthSizeClass.Medium, hasStableMasterDetailContext = true),
        )
    }

    @Test
    fun largeSurfaceGetsAConstrainedWidthAndResponsivePadding() {
        val regular = adaptiveSurfacePolicy(1200.dp, 800.dp, fontScale = 1f)
        val largeFont = adaptiveSurfacePolicy(1200.dp, 800.dp, fontScale = 1.5f)

        assertEquals(720.dp, regular.maxWidth)
        assertEquals(24.dp, regular.horizontalPadding)
        assertEquals(800.dp, largeFont.maxWidth)
        assertEquals(16.dp, largeFont.horizontalPadding)
    }

    @Test
    fun compactSurfaceDoesNotOverflowItsWindow() {
        val policy = adaptiveSurfacePolicy(390.dp, 844.dp, fontScale = 1.5f)

        assertEquals(390.dp, policy.maxWidth)
        assertEquals(16.dp, policy.horizontalPadding)
        assertEquals(false, policy.isLandscape)
    }

    @Test
    fun landscapeIsDerivedFromBothWindowDimensions() {
        assertEquals(true, adaptiveSurfacePolicy(1024.dp, 600.dp, fontScale = 1f).isLandscape)
        assertEquals(false, adaptiveSurfacePolicy(600.dp, 1024.dp, fontScale = 1f).isLandscape)
    }

    @Test
    fun paneSelectionSurvivesResizeButClearsWhenContactDisappears() {
        assertEquals(
            "contact-1",
            retainPaneSelection("contact-1", listOf("contact-1", "contact-2")),
        )
        assertEquals(
            null,
            retainPaneSelection("contact-1", listOf("contact-2")),
        )
    }
}