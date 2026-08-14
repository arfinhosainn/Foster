package app.usenekko.onboarding

import app.usenekko.onboarding.group.groupAvatarIds
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupSelectionTest {

    @Test
    fun selectedAvatarIsShownOnlyInTheSelectedGroup() {
        assertEquals(listOf("2"), groupAvatarIds("family", "family", "2"))
        assertEquals(emptyList(), groupAvatarIds("friends", "family", "2"))
    }

    @Test
    fun noSelectedAvatarKeepsTheGroupEmpty() {
        assertEquals(emptyList(), groupAvatarIds("family", "family", null))
        assertEquals(emptyList(), groupAvatarIds("family", "family", ""))
    }

    @Test
    fun selectingAnotherGroupMovesTheAvatar() {
        assertEquals(emptyList(), groupAvatarIds("family", "friends", "1"))
        assertEquals(listOf("1"), groupAvatarIds("friends", "friends", "1"))
    }
}