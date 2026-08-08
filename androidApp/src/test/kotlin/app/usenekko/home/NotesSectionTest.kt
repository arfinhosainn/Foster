package app.usenekko.home

import app.usenekko.home.domain.Note
import app.usenekko.home.presentation.contactprofile.noteGridRows
import app.usenekko.home.presentation.contactprofile.shouldPlaceAddNoteBesideLastNote
import app.usenekko.home.presentation.contactprofile.shouldShowLargeAddNoteCard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesSectionTest {

    @Test
    fun emptyStateDoesNotShowLargeAddNoteCard() {
        assertFalse(shouldShowLargeAddNoteCard(emptyList()))
    }

    @Test
    fun populatedStateShowsLargeAddNoteCard() {
        assertTrue(
            shouldShowLargeAddNoteCard(
                listOf(
                    Note(
                        id = "note-1",
                        contactId = "contact-1",
                        title = "First note",
                        body = "A memory",
                        createdAt = "2026-08-08T12:00:00Z",
                    ),
                ),
            ),
        )
    }

    @Test
    fun noteGridRowsPlacesTwoNotesInEachRow() {
        val notes = listOf(
            note("note-1"),
            note("note-2"),
            note("note-3"),
        )

        assertEquals(
            listOf(
                listOf("note-1", "note-2"),
                listOf("note-3"),
            ),
            noteGridRows(notes).map { row -> row.map(Note::id) },
        )
    }

    @Test
    fun addNoteCardUsesTheOpenCellBesideAnOddLastNote() {
        assertTrue(shouldPlaceAddNoteBesideLastNote(listOf(note("note-1"))))
        assertTrue(
            shouldPlaceAddNoteBesideLastNote(
                listOf(note("note-1"), note("note-2"), note("note-3")),
            ),
        )
        assertFalse(
            shouldPlaceAddNoteBesideLastNote(
                listOf(note("note-1"), note("note-2")),
            ),
        )
    }

    private fun note(id: String) = Note(
        id = id,
        contactId = "contact-1",
        title = "Note",
        body = "A memory",
        createdAt = "2026-08-08T12:00:00Z",
    )
}