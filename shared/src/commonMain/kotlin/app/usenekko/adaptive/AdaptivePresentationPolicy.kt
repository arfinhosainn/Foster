package app.usenekko.adaptive

enum class AdaptivePresentation {
    SinglePane,
    SupportingPane,
}

fun contactPresentation(widthSizeClass: WindowWidthSizeClass): AdaptivePresentation =
    if (widthSizeClass == WindowWidthSizeClass.Expanded) {
        AdaptivePresentation.SupportingPane
    } else {
        AdaptivePresentation.SinglePane
    }

/**
 * Group management remains single-pane while its detail content is owned by
 * the existing modal bottom sheet rather than a stable route-level master/detail
 * surface.
 */
fun groupPresentation(
    widthSizeClass: WindowWidthSizeClass,
    hasStableMasterDetailContext: Boolean,
): AdaptivePresentation =
    if (widthSizeClass == WindowWidthSizeClass.Expanded && hasStableMasterDetailContext) {
        AdaptivePresentation.SupportingPane
    } else {
        AdaptivePresentation.SinglePane
    }