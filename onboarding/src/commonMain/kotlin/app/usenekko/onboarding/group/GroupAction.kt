package app.usenekko.onboarding.group

/**
 * Actions that the group onboarding screen can dispatch.
 */
sealed interface GroupAction {
    /** User tapped a specific group card to toggle selection. */
    data class ToggleGroup(val groupId: String) : GroupAction

    /** User tapped the "create new group" button. */
    data object CreateNewGroup : GroupAction
}
