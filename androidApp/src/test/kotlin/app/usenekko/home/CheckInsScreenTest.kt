package app.usenekko.home

import app.usenekko.home.presentation.resolveCheckInsAddClick
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