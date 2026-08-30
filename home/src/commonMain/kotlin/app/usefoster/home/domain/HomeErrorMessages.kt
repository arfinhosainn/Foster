package app.usefoster.home.domain

import app.usefoster.shared.domain.ProfileError
import foster.home.generated.resources.Res
import foster.home.generated.resources.error_delete_account
import foster.home.generated.resources.error_network
import foster.home.generated.resources.error_session_expired
import foster.home.generated.resources.error_unexpected
import org.jetbrains.compose.resources.StringResource

fun ContactError.toUserMessage(): String = when (this) {
    ContactError.NotAuthenticated -> "Your session expired. Please sign in again."
    ContactError.Network -> "Check your connection and try again."
    is ContactError.Unknown -> "Something went wrong. Please try again."
}

fun BrainstormError.toUserMessage(): String = when (this) {
    BrainstormError.NotAuthenticated -> "Your session expired. Please sign in again."
    BrainstormError.Network -> "Check your connection and try again."
    is BrainstormError.Unknown -> "Something went wrong. Please try again."
}

fun DeleteAccountError.toUserMessage(): String = when (this) {
    DeleteAccountError.NotAuthenticated -> "Your session expired. Please sign in again."
    DeleteAccountError.Network -> "Check your connection and try again."
    is DeleteAccountError.Unknown -> "Something went wrong. Your account was not deleted."
}

fun ProfileError.toUserMessage(): String = when (this) {
    ProfileError.NotAuthenticated -> "Your session expired. Please sign in again."
    ProfileError.Network -> "Check your connection and try again."
    is ProfileError.Unknown -> "Something went wrong. Please try again."
}

fun ContactError.toUserMessageResource(): StringResource = when (this) {
    ContactError.NotAuthenticated -> Res.string.error_session_expired
    ContactError.Network -> Res.string.error_network
    is ContactError.Unknown -> Res.string.error_unexpected
}

fun BrainstormError.toUserMessageResource(): StringResource = when (this) {
    BrainstormError.NotAuthenticated -> Res.string.error_session_expired
    BrainstormError.Network -> Res.string.error_network
    is BrainstormError.Unknown -> Res.string.error_unexpected
}

fun DeleteAccountError.toUserMessageResource(): StringResource = when (this) {
    DeleteAccountError.NotAuthenticated -> Res.string.error_session_expired
    DeleteAccountError.Network -> Res.string.error_network
    is DeleteAccountError.Unknown -> Res.string.error_delete_account
}

fun ProfileError.toUserMessageResource(): StringResource = when (this) {
    ProfileError.NotAuthenticated -> Res.string.error_session_expired
    ProfileError.Network -> Res.string.error_network
    is ProfileError.Unknown -> Res.string.error_unexpected
}

fun String.toAccountErrorResource(): StringResource = when {
    equals("Not authenticated", ignoreCase = true) -> Res.string.error_session_expired
    startsWith("Check your connection", ignoreCase = true) -> Res.string.error_network
    else -> Res.string.error_unexpected
}