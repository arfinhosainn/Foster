package app.usefoster.shared.swipehint

interface SwipeHintPreferenceDataSource {
    suspend fun hasSeenHint(): Boolean
    suspend fun setHasSeenHint(seen: Boolean)
    suspend fun hasSwipedBook(): Boolean
    suspend fun setHasSwipedBook(swiped: Boolean)
}