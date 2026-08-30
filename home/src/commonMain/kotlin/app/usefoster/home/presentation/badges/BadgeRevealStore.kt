package app.usefoster.home.presentation.badges

import app.usefoster.home.domain.Badge
import app.usefoster.home.domain.ContactDataSource
import app.usefoster.shared.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-global channel for "a badge was just unlocked" events. The check-in
 * ViewModels publish here; the overlay hosted at the app root (OnboardingApp)
 * observes it and consumes the badge once the user dismisses the reveal.
 */
object BadgeRevealStore {
    private val _pending = MutableStateFlow<Badge?>(null)
    val pending: StateFlow<Badge?> = _pending.asStateFlow()

    fun trigger(badge: Badge) {
        _pending.value = badge
    }

    fun consume() {
        _pending.value = null
    }
}

/**
 * The badge IDs currently unlocked, or `null` when the query failed (used to
 * distinguish "definitely zero unlocks" from "we don't know the previous state").
 */
suspend fun ContactDataSource.unlockedBadgeIdsOrNull(): Set<String>? =
    (getUserBadges() as? Result.Success)?.data?.mapTo(mutableSetOf()) { it.badgeId }

/**
 * Pure (testable) diff: of the badges unlocked in [afterIds] but not in
 * [previousIds], pick the lowest-threshold one — that is the badge the user
 * just earned.
 */
fun newlyUnlockedBadge(
    previousIds: Set<String>,
    afterIds: Set<String>,
    badges: List<Badge>,
): Badge? {
    val newly = afterIds.filterNot { it in previousIds }
    if (newly.isEmpty()) return null
    return badges.filter { it.id in newly }.minByOrNull { it.threshold }
}

/**
 * Fetches the current unlock set, diffs it against [previousBadgeIds], and —
 * if a new badge appeared — publishes it to [BadgeRevealStore]. No-op when the
 * "before" state was unknown or no new badge is present.
 */
suspend fun ContactDataSource.detectAndTriggerBadgeReveal(previousBadgeIds: Set<String>) {
    val afterIds = unlockedBadgeIdsOrNull() ?: return
    val badge = newlyUnlockedBadge(previousBadgeIds, afterIds, getBadgesOrEmpty())
        ?: return
    BadgeRevealStore.trigger(badge)
}

private suspend fun ContactDataSource.getBadgesOrEmpty(): List<Badge> =
    (getBadges() as? Result.Success)?.data.orEmpty()
