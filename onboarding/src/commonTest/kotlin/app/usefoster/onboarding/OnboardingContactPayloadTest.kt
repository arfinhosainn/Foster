package app.usefoster.onboarding

import app.usefoster.onboarding.data.supabase.toCompleteOnboardingPayload
import app.usefoster.onboarding.data.supabase.dto.EnsureProfilePayload
import app.usefoster.onboarding.domain.CustomReminderDraft
import app.usefoster.onboarding.domain.GroupDraft
import app.usefoster.onboarding.domain.NoteDraft
import app.usefoster.onboarding.domain.OnboardingDraft
import app.usefoster.onboarding.domain.ReminderFrequency
import app.usefoster.onboarding.domain.ReminderTimeDraft
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

    @Test
    fun onboardingContentIsSentWithoutAContactIdForTheRpcToAttach() {
        val payload = OnboardingDraft(
            contactName = "Alex",
            reminderFrequency = ReminderFrequency.Weekly,
            reminderTime = ReminderTimeDraft(hour = 9, minute = 30),
            customReminders = listOf(
                CustomReminderDraft(
                    id = "reminder-1",
                    title = "Birthday",
                    description = "Send a card",
                ),
            ),
            notes = listOf(NoteDraft(id = "note-1", title = "Favorite food", body = "Pizza")),
        ).toCompleteOnboardingPayload(email = null, emailVerified = false)

        assertEquals("Weekly", payload.reminderFrequency)
        assertEquals(9, payload.reminderHour)
        assertEquals(30, payload.reminderMinute)
        assertEquals(1, payload.customReminders.size)
        assertEquals(1, payload.notes.size)

        val json = Json.encodeToJsonElement(payload).jsonObject
        assertTrue("contactId" !in json)
        assertTrue("contactId" !in json.getValue("customReminders").toString())
        assertTrue("contactId" !in json.getValue("notes").toString())
    }

    @Test
    fun ensureProfilePayloadHasAnExplicitSerializableShape() {
        val payload = EnsureProfilePayload(
            id = "user-1",
            onboardingStep = 1,
        )

        val json = Json.encodeToJsonElement(payload).jsonObject

        assertEquals("user-1", json.getValue("id").toString().trim('"'))
        assertEquals("1", json.getValue("onboarding_step").toString())
    }
}