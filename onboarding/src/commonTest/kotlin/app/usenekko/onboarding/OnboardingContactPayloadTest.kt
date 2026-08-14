package app.usenekko.onboarding

import app.usenekko.onboarding.data.supabase.toCompleteOnboardingPayload
import app.usenekko.onboarding.domain.GroupDraft
import app.usenekko.onboarding.domain.OnboardingDraft
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingContactPayloadTest {

    @Test
    fun completionPayloadContainsDataNeededToCreateHomeContactMembership() {
        val payload = OnboardingDraft(
            contactName = "Alex",
            selectedAvatarId = "2",
            selectedGroupId = "family",
        ).toCompleteOnboardingPayload(
            email = "alex@example.com",
            emailVerified = true,
        )

        val json = Json.encodeToJsonElement(payload).jsonObject

        assertTrue("selectedGroupName" in json)
        assertTrue("selectedAvatarColor" in json)
        assertEquals("Family", payload.selectedGroupName)
        assertEquals("#FF9500", payload.selectedAvatarColor)
        assertEquals(setOf("Family", "Friends"), payload.groups.map { it.name }.toSet())
    }

    @Test
    fun customSelectedGroupIsSentByName() {
        val payload = OnboardingDraft(
            contactName = "Alex",
            selectedGroupId = "group_1",
            groups = listOf(GroupDraft(id = "group_1", name = "Work")),
        ).toCompleteOnboardingPayload(email = null, emailVerified = false)

        assertEquals("Work", payload.selectedGroupName)
    }
}