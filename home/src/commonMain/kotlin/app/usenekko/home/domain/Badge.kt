package app.usenekko.home.domain

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val threshold: Int,
)

data class UserBadge(
    val badgeId: String,
    val unlockedAt: String,
)

data class BadgeSlot(
    val badge: Badge,
    val unlocked: Boolean,
)
