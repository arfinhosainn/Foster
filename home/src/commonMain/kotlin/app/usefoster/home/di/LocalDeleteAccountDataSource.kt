package app.usefoster.home.di

import androidx.compose.runtime.staticCompositionLocalOf
import app.usefoster.home.domain.DeleteAccountDataSource

val LocalDeleteAccountDataSource = staticCompositionLocalOf<DeleteAccountDataSource> {
    error("DeleteAccountDataSource not provided")
}
