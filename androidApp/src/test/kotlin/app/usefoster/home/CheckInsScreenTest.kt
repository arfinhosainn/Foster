package app.usefoster.home

import app.usefoster.home.presentation.resolveCheckInsAddClick
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInsScreenTest {

    @Test
    fun defaultAddActionOpensContactSheetInsteadOfNavigatingHome() {
        var openedAddContact = false

        resolveCheckInsAddClick(onAddClick = null) {
            openedAddContact = true
        }.invoke()

        assertTrue(openedAddContact)
    }
}