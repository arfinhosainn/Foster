package app.usefoster.home.presentation.history

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import app.usefoster.navigation.NavAnimationSpecs
import app.usefoster.navigation.Navigator
import app.usefoster.navigation.Screen
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged

/** Page index of the History page inside the Home/History book (left page). */
const val HISTORY_BOOK_HISTORY_PAGE = 0

/** Page index of the Home page inside the Home/History book (right page). */
const val HISTORY_BOOK_HOME_PAGE = 1

/**
 * Tap-to-open animation (status card, top-bar back) — matches the app's
 * horizontal route transition so programmatic focus flips feel identical to
 * the rest of the app. Drag settle keeps the pager's own spring on purpose:
 * a gesture resolves with a spring, a tap resolves with a tween.
 */
private val BookTapScrollSpec = tween<Float>(
    durationMillis = NavAnimationSpecs.HorizontalDurationMillis,
    easing = FastOutSlowInEasing,
)

/** What the shell must do after the book settles on a page. */
sealed interface HomeBookAction {
    /** History page is focused but History is not the current route — push it. */
    data object OpenHistory : HomeBookAction

    /** Home page is focused while History is the current route — leave it. */
    data object ReturnToHome : HomeBookAction
}

/**
 * Pure settle → route policy. Only fires when the route actually DISAGREES
 * with the settled page, which makes the two-way sync echo-free: tapping the
 * status card pushes History and animates the pager there; the settle event
 * then finds the route already in place and maps to null.
 */
fun homeBookActionFor(settledPage: Int, currentScreen: Screen): HomeBookAction? = when {
    settledPage == HISTORY_BOOK_HISTORY_PAGE && currentScreen !is Screen.CheckInHistory ->
        HomeBookAction.OpenHistory

    settledPage == HISTORY_BOOK_HOME_PAGE && currentScreen is Screen.CheckInHistory ->
        HomeBookAction.ReturnToHome

    else -> null
}

/** Page the book should show for [screen]. */
fun historyBookTargetPage(screen: Screen): Int =
    if (screen is Screen.CheckInHistory) HISTORY_BOOK_HISTORY_PAGE else HISTORY_BOOK_HOME_PAGE

/**
 * Whether leaving History for Home can pop the pushed History route (Home sits
 * directly beneath it). If History somehow became the stack root — deep link,
 * process-death restore — popping would strand the user on an empty stack, so
 * the shell must REPLACE it with Home instead.
 */
fun historyIsPoppedOnReturn(backStack: List<Screen>): Boolean =
    backStack.size >= 2 && backStack[backStack.size - 2] is Screen.Home

/**
 * 0f when focus is on Home, 1f on History — finger-tracking progress of the
 * book, derived from the pager's live offset so surfaces outside the pager
 * (the bottom bar) can follow the drag instead of popping at the settle
 * boundary.
 */
fun historyBookProgress(pagerState: PagerState): Float {
    val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
    return (1f - abs(position - HISTORY_BOOK_HISTORY_PAGE)).coerceIn(0f, 1f)
}

/**
 * The Home ↔ History "book": both screens stay composed as pages of ONE
 * [HorizontalPager], so swiping is a focus drag between already-open pages —
 * not a route push. The route stack remains the source of truth:
 *
 *  - route → pager: pushing/popping History animates the book (guarded: only
 *    when the pager actually disagrees, never mid-gesture).
 *  - pager → route: settling on a page reconciles the route (guarded: only
 *    when the route disagrees, so programmatic scrolls never echo back).
 *
 * Any divergence — interrupted flings, grabbing the pager mid-tap-animation —
 * self-corrects on the next settle.
 *
 * [pagerState] is hoisted by the shell so the bottom bar can read the live
 * drag offset. History is the LEFT page (page 0); in RTL locales the pager
 * mirrors visually, which is the desired reading-order behavior.
 */
@Composable
fun HomeHistoryBook(
    navigator: Navigator,
    pagerState: PagerState,
    historyPage: @Composable () -> Unit,
    homePage: @Composable () -> Unit,
    isHistoryAccessible: Boolean = true,
    onHistoryLocked: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Pager settle → route. The policy only dispatches when the route
    // disagrees with the settled page, so this can never loop with the
    // route → pager effect below.
    LaunchedEffect(navigator, isHistoryAccessible, onHistoryLocked) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                when (homeBookActionFor(page, navigator.currentScreen)) {
                    HomeBookAction.OpenHistory -> {
                        if (isHistoryAccessible) {
                            navigator.navigate(Screen.CheckInHistory)
                        } else {
                            onHistoryLocked()
                            pagerState.animateScrollToPage(
                                page = HISTORY_BOOK_HOME_PAGE,
                                animationSpec = BookTapScrollSpec,
                            )
                        }
                    }
                    HomeBookAction.ReturnToHome -> {
                        if (historyIsPoppedOnReturn(navigator.backStack)) {
                            navigator.goBack()
                        } else {
                            navigator.replace(Screen.Home)
                        }
                    }

                    null -> Unit
                }
            }
    }

    // Route → pager. Skipped mid-gesture; whatever divergence remains is
    // reconciled by the settle effect once the drag finishes.
    LaunchedEffect(navigator.navState) {
        val target = historyBookTargetPage(navigator.currentScreen)
        if (pagerState.settledPage != target && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(page = target, animationSpec = BookTapScrollSpec)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        // Both pages stay composed ("two pages of an open book"): no empty
        // History flash mid-drag, scroll positions survive every swipe, and
        // both ViewModels come alive at book entry regardless of focus.
        beyondViewportPageCount = 1,
    ) { page ->
        when (page) {
            HISTORY_BOOK_HISTORY_PAGE -> historyPage()
            else -> homePage()
        }
    }
}