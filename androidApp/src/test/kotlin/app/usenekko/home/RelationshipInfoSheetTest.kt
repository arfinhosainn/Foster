package app.usenekko.home

import app.usenekko.home.presentation.contactprofile.grassStageForCheckInCount
import app.usenekko.home.presentation.contactprofile.checkInCountText
import app.usenekko.home.presentation.components.avatarIndexForId
import org.junit.Assert.assertEquals
import org.junit.Test

class RelationshipInfoSheetTest {

    @Test
    fun checkInCountIsDisplayedSeparatelyFromItsLabel() {
        assertEquals("15", checkInCountText(15))
    }

    @Test
    fun selectedAvatarIdsMapToBuiltInAvatarPositions() {
        assertEquals(0, avatarIndexForId("0"))
        assertEquals(5, avatarIndexForId("5"))
        assertEquals(null, avatarIndexForId("6"))
        assertEquals(null, avatarIndexForId("invalid"))
    }

    @Test
    fun grassProgressStartsAtSmallestStage() {
        assertEquals(1, grassStageForCheckInCount(0))
        assertEquals(1, grassStageForCheckInCount(-1))
    }

    @Test
    fun grassProgressGrowsAtConfiguredBoundaries() {
        assertEquals(2, grassStageForCheckInCount(1))
        assertEquals(2, grassStageForCheckInCount(2))
        assertEquals(3, grassStageForCheckInCount(3))
        assertEquals(3, grassStageForCheckInCount(5))
        assertEquals(4, grassStageForCheckInCount(6))
        assertEquals(4, grassStageForCheckInCount(9))
    }

    @Test
    fun grassProgressStopsAtLargestStage() {
        assertEquals(5, grassStageForCheckInCount(10))
        assertEquals(5, grassStageForCheckInCount(100))
    }
}