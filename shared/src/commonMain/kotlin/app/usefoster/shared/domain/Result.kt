package app.usefoster.shared.domain

sealed interface Result<out D, out E> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E>(val error: E) : Result<Nothing, E>
}

typealias EmptyResult<E> = Result<Unit, E>
