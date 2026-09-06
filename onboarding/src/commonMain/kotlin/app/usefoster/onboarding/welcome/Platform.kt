package app.usefoster.onboarding.welcome

/**
 * Whether the app is running on an Apple platform (iOS).
 *
 * Used to show platform-appropriate sign-in options: iOS shows both
 * "Continue with Apple" and "Continue with Google", while Android shows
 * only the Google option.
 */
expect fun isApplePlatform(): Boolean