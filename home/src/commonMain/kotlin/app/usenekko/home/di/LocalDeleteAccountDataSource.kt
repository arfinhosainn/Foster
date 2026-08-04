package app.usenekko.home.di

import androidx.compose.runtime.staticCompositionLocalOf
import app.usenekko.home.domain.DeleteAccountDataSource

val LocalDeleteAccountDataSource = staticCompositionLocalOf<DeleteAccountDataSource> {
    error("DeleteAccountDataSource not provided")
}
