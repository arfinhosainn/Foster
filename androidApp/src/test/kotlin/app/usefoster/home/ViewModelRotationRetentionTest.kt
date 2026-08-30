package app.usefoster.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.usefoster.home.addcontact.AddContactViewModel
import app.usefoster.home.di.addContactViewModelFactory
import app.usefoster.home.domain.CheckIn
import app.usefoster.home.domain.Group
import app.usefoster.home.domain.GroupMembership
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ViewModelRotationRetentionTest {

    @Test
    fun addContactDraftSurvivesActivityRecreation() {
        val dataSource = FakeContactDataSource(
            contacts = emptyList(),
            groups = listOf(Group("group-1", "Family")),
            memberships = listOf(GroupMembership("contact-1", "group-1")),
            checkIns = listOf(CheckIn("check-in-1", "contact-1", "2026-08-14T12:00:00Z")),
        )
        val store = ViewModelStore()
        val factory = addContactViewModelFactory(
            contactDataSource = dataSource,
            subscriptionRepository = FakeSubscriptionRepository(),
        )

        val beforeRotation = ViewModelProvider.create(store, factory)[AddContactViewModel::class.java]
        beforeRotation.onNameChanged("Alice")
        beforeRotation.onGroupSelected("group-1")
        beforeRotation.onNextStep()

        val afterRotation = ViewModelProvider.create(store, factory)[AddContactViewModel::class.java]

        assertSame(beforeRotation, afterRotation)
        assertEquals("Alice", afterRotation.state.value.name)
        assertEquals("group-1", afterRotation.state.value.selectedGroupId)
        assertEquals(1, afterRotation.state.value.currentStep)

        store.clear()
    }
}