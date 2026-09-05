package app.usefoster.home.presentation.brainstorm

import app.usefoster.home.domain.BrainstormTopic

/**
 * Formats a brainstorm topic into a plain-text, paste-ready message for the
 * share sheet and clipboard: **only the idea's description body** — the title
 * is card UI, not part of the message. No markdown, no quotes, no UI chrome,
 * no signature. A blank description yields an empty string.
 */
fun formatTopicMessage(topic: BrainstormTopic): String =
    topic.description?.trim().orEmpty()