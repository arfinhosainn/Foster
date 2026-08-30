package app.usefoster.adaptive

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class WindowWidthSizeClassTest {
    @Test
    fun classifiesCompactWidths() {
        assertEquals(WindowWidthSizeClass.Compact, windowWidthSizeClass(599.dp))
    }

    @Test
    fun classifiesMediumWidths() {
        assertEquals(WindowWidthSizeClass.Medium, windowWidthSizeClass(600.dp))
        assertEquals(WindowWidthSizeClass.Medium, windowWidthSizeClass(839.dp))
    }

    @Test
    fun classifiesExpandedWidths() {
        assertEquals(WindowWidthSizeClass.Expanded, windowWidthSizeClass(840.dp))
        assertEquals(WindowWidthSizeClass.Expanded, windowWidthSizeClass(1200.dp))
    }
}