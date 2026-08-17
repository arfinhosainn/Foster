package app.usenekko.onboarding.customreminder

data class CustomReminderState(
    val reminders: List<ReminderItem> = emptyList(),
    val isBottomSheetVisible: Boolean = false,
    val draftTitle: String = "",
    val draftDescription: String = "",
    val draftRecurrence: String = "None",
    val draftDate: String = "Choose Date",
    val draftDateEpochMillis: Long? = null,
    val editingReminderId: String? = null,
)

data class ReminderItem(
    val id: String,
    val title: String,
    val description: String,
    val recurrence: String,
    val date: String
)
