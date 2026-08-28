package app.usenekko.home.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.home.di.LocalDeleteAccountDataSource
import app.usenekko.home.domain.DeleteAccountError
import app.usenekko.home.presentation.settings.components.AppearanceBottomSheet
import app.usenekko.home.presentation.settings.components.DeleteAccountBottomSheet
import app.usenekko.home.presentation.settings.components.PremiumCard
import app.usenekko.home.presentation.settings.components.SettingsGroup
import app.usenekko.home.presentation.settings.components.SettingsRow
import app.usenekko.home.presentation.settings.components.SettingsTopBar
import app.usenekko.shared.domain.Result
import app.usenekko.shared.notifications.ReminderScheduler
import app.usenekko.theme.AppThemeMode
import app.usenekko.theme.LocalThemeStore
import app.usenekko.theme.NekkoTheme
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.coroutines.launch
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_appearance
import nekko.home.generated.resources.ic_contacts
import nekko.home.generated.resources.ic_greenprofile
import nekko.home.generated.resources.ic_groups
import nekko.home.generated.resources.ic_notification
import nekko.home.generated.resources.ic_privacy
import nekko.home.generated.resources.ic_support
import nekko.home.generated.resources.ic_terms
import nekko.home.generated.resources.ic_trashbin
import org.jetbrains.compose.resources.vectorResource
import nekko.home.generated.resources.settings_account
import nekko.home.generated.resources.settings_appearance
import nekko.home.generated.resources.settings_contacts
import nekko.home.generated.resources.settings_danger_zone
import nekko.home.generated.resources.settings_delete_account
import nekko.home.generated.resources.settings_groups
import nekko.home.generated.resources.settings_notification
import nekko.home.generated.resources.settings_support
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingScreen(
    onBack: () -> Unit,
    onAccountClick: () -> Unit = {},
    onPremiumClick: () -> Unit = {},
    onAccountDeleted: () -> Unit = {},
    initiallyShowAccountSheet: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val liquidState = rememberLiquidState()
    val density = LocalDensity.current

    // Real OS notification permission state. Read on launch and after returning
    // from the OS settings screen. Android/iOS can't re-grant from an in-app
    // toggle once denied, so tapping the row opens the system settings instead.
    val reminderScheduler = remember { ReminderScheduler() }
    val scope = rememberCoroutineScope()
    var notificationEnabled by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        notificationEnabled = reminderScheduler.isEnabled()
    }

    // Appearance — local theme preference (Dark/Light/System)
    val themeStore = LocalThemeStore.current
    val selectedMode by themeStore?.mode?.collectAsState()
        ?: remember { mutableStateOf(AppThemeMode.SYSTEM) }
    var showAppearanceSheet by rememberSaveable { mutableStateOf(false) }
    // Opens pre-opened when arriving via the plant-reward "collect" flow
    // (Screen.Settings(openAccountSheet = true)). The LaunchedEffect re-asserts
    // it because rememberSaveable can restore showAccountSheet=false from a
    // previous Settings visit, which would swallow the arrival flag.
    var showAccountSheet by rememberSaveable { mutableStateOf(initiallyShowAccountSheet) }
    LaunchedEffect(initiallyShowAccountSheet) {
        if (initiallyShowAccountSheet) showAccountSheet = true
    }
    var showGroupSheet by rememberSaveable { mutableStateOf(false) }

    // Delete Account — destructive & irreversible. Gated behind a typed
    // confirmation sheet; only signs out + routes to Welcome on real success.
    val deleteAccountDataSource = LocalDeleteAccountDataSource.current
    var showDeleteSheet by rememberSaveable { mutableStateOf(false) }
    var deleteLoading by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    fun confirmDeleteAccount() {
        scope.launch {
            deleteLoading = true
            deleteError = null
            when (val result = deleteAccountDataSource.deleteAccount()) {
                is Result.Success -> {
                    deleteLoading = false
                    onAccountDeleted()
                }

                is Result.Error -> {
                    deleteLoading = false
                    deleteError = when (val err = result.error) {
                        is DeleteAccountError.Network ->
                            "Network error. Check your connection and try again."

                        is DeleteAccountError.NotAuthenticated ->
                            "Your session expired. Please sign in again."

                        is DeleteAccountError.Unknown ->
                            err.detail?.takeIf { it.isNotBlank() }
                                ?: "Something went wrong. Your account was not deleted."
                    }
                }
            }
        }
    }


    val appearanceLabel = when (selectedMode) {
        AppThemeMode.DARK -> "Dark"
        AppThemeMode.LIGHT -> "Light"
        AppThemeMode.SYSTEM -> "System"
    }

    // Store in pixels, convert to Dp only when needed
    var topBarHeightPx by remember { mutableStateOf(0) }
    val topBarHeightDp by remember(topBarHeightPx) {
        derivedStateOf { with(density) { topBarHeightPx.toDp() } }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val backgroundModifier =
            if (showAccountSheet || showGroupSheet) Modifier.blur(5.dp) else Modifier

        Box(
            Modifier
                .matchParentSize()
                .then(backgroundModifier)
                .background(NekkoTheme.colors.background.b0)
        )

        // Scrollable content — padded so initial position clears the top bar
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter)
                .background(NekkoTheme.colors.background.b0)
                .then(backgroundModifier)
                .padding(
                    top = topBarHeightDp, start = 24.dp, end =
                        24.dp
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            Spacer(Modifier.height(12.dp)) // small breathing room below bar

            PremiumCard(onClick = onPremiumClick)

            SettingsGroup(
                liquidState = liquidState,
                rows = listOf(
                    SettingsRow.Item(icon = Res.drawable.ic_greenprofile, title = stringResource(Res.string.settings_account)) {
                        showAccountSheet = true
                        onAccountClick()
                    },
                    SettingsRow.Item(
                        icon = Res.drawable.ic_appearance,
                        title = stringResource(Res.string.settings_appearance),
                        trailing = appearanceLabel,
                    ) { showAppearanceSheet = true },
                    SettingsRow.Item(
                        icon = Res.drawable.ic_notification,
                        title = stringResource(Res.string.settings_notification),
                        trailing = when (notificationEnabled) {
                            true -> "On"
                            false -> "Off"
                            null -> null
                        },
                    ) {
                        // Opens the OS notification settings page — can't be toggled
                        // in-app once denied at the system level.
                        scope.launch {
                            reminderScheduler.openSettings()
                        }
                    },
                    SettingsRow.Item(icon = Res.drawable.ic_contacts, title = stringResource(Res.string.settings_contacts)) {},
                    SettingsRow.Item(icon = Res.drawable.ic_groups, title = stringResource(Res.string.settings_groups)) {
                        showGroupSheet = true
                    },
                    SettingsRow.Item(icon = Res.drawable.ic_support, title = stringResource(Res.string.settings_support)) {},
                ),
            )

            // "Danger Zone" label
            Text(
                text = stringResource(Res.string.settings_danger_zone),
                style = NekkoTheme.typography.heading3,
                fontWeight = FontWeight.SemiBold,
                color = NekkoTheme.colors.text.primary,
                modifier = Modifier.padding(horizontal = 10.dp),
            )

            SettingsGroup(
                liquidState = liquidState,
                rows = listOf(
                    SettingsRow.Destructive(
                        icon = Res.drawable.ic_trashbin,
                        title = stringResource(Res.string.settings_delete_account)
                    ) {
                        deleteError = null
                        showDeleteSheet = true
                    },
                ),
            )

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Nekko 2026.01.10",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = NekkoTheme.colors.text.tertiary
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_terms),
                        contentDescription = "",
                        tint = Color.Unspecified
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Terms of Service",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = NekkoTheme.colors.text.secondary
                    )

                }

                Spacer(Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_privacy),
                        contentDescription = "",
                        tint = Color.Unspecified
                    )
                    Spacer(Modifier.width(5.dp))

                    Text(
                        "Privacy",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = NekkoTheme.colors.text.secondary
                    )

                    Spacer(Modifier.height(34.dp))

                }

            }

        }

        // Top bar — measured in px, stored correctly
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(backgroundModifier)
                .onSizeChanged { topBarHeightPx = it.height },
        ) {
            SettingsTopBar(onBack = onBack)
        }

        // Appearance picker
        if (showAppearanceSheet) {
            AppearanceBottomSheet(
                selectedMode = selectedMode,
                onSelect = { mode -> themeStore?.setMode(mode) },
                onDismiss = { showAppearanceSheet = false },
            )
        }

        if (showAccountSheet) {
            AccountBottomSheet(onDismiss = { showAccountSheet = false })
        }

        if (showGroupSheet) {
            GroupBottomSheet(onDismiss = { showGroupSheet = false })
        }

        // Delete Account — typed-confirmation sheet (destructive, irreversible)
        if (showDeleteSheet) {
            DeleteAccountBottomSheet(
                isLoading = deleteLoading,
                errorMessage = deleteError,
                onConfirm = ::confirmDeleteAccount,
                onDismiss = {
                    if (!deleteLoading) {
                        showDeleteSheet = false
                        deleteError = null
                    }
                },
            )
        }

        // Scrim — taller, non-linear fade for a polished iOS-style effect
        if (topBarHeightPx > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // taller = smoother, more natural fade
                    .align(Alignment.TopCenter)
                    .then(backgroundModifier)
                    .offset(y = topBarHeightDp)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to NekkoTheme.colors.background.b0,
                                0.4f to NekkoTheme.colors.background.b0.copy(alpha = 0.85f),
                                0.75f to NekkoTheme.colors.background.b0.copy(alpha = 0.3f),
                                1.0f to NekkoTheme.colors.background.b0.copy(alpha = 0f),
                            ),
                        ),
                    ),
            )
        }

    }
}


@PreviewLightDark
@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
@Composable
fun PreviewSettingScreen() = NekkoTheme {
    SettingScreen(onBack = {})
}