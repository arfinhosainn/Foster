package app.usefoster.home.presentation.brainstorm

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.usefoster.designsystem.snackbar.FosterSnackbarHost
import app.usefoster.home.di.rememberBrainstormViewModel
import app.usefoster.home.domain.BrainstormTopic
import app.usefoster.home.presentation.brainstorm.components.BrainstormTabs
import app.usefoster.home.presentation.brainstorm.components.BrainstormTopBar
import app.usefoster.home.presentation.brainstorm.components.CurrentOutputContent
import app.usefoster.home.presentation.brainstorm.components.HistoryContent
import app.usefoster.shared.messaging.rememberShareComposer
import app.usefoster.theme.FosterTheme
import foster.home.generated.resources.Res
import foster.home.generated.resources.brainstorm_share_unavailable
import foster.home.generated.resources.brainstorm_copied
import foster.home.generated.resources.gradients
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BrainstormScreen(
    contactId: String,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberBrainstormViewModel(contactId)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedTab by remember { mutableStateOf(BrainstormTab.CurrentOutput) }

    val shareUnavailableMessage = stringResource(Res.string.brainstorm_share_unavailable)
    val copiedMessage = stringResource(Res.string.brainstorm_copied)
    val shareComposer = rememberShareComposer(
        onUnavailable = { viewModel.onAction(BrainstormAction.ShowNotice(shareUnavailableMessage)) },
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val shareTopic: (BrainstormTopic) -> Unit = { topic ->
        val text = formatTopicMessage(topic)
        if (text.isBlank()) {
            viewModel.onAction(BrainstormAction.ShowNotice(shareUnavailableMessage))
        } else {
            shareComposer(text)
        }
    }
    @Suppress("DEPRECATION") // LocalClipboardManager.setText remains the common KMP API here.
    val copyTopic: (BrainstormTopic) -> Unit = { topic ->
        val text = formatTopicMessage(topic)
        if (text.isNotBlank()) {
            clipboard.setText(AnnotatedString(text))
            scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.refreshIfStale()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FosterTheme.colors.background.b0),
    ) {
        Image(
            painter = painterResource(Res.drawable.gradients),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
        )
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { BrainstormTopBar() },
            snackbarHost = {
                FosterSnackbarHost(hostState = snackbarHostState)
            },
            containerColor = FosterTheme.colors.background.b0.copy(alpha = 0f),
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                if (state.isRefreshing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = FosterTheme.colors.text.tertiary,
                            strokeWidth = 1.5.dp,
                        )
                    }
                }
                BrainstormTabs(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                )
                when (selectedTab) {
                    BrainstormTab.CurrentOutput -> CurrentOutputContent(
                        topics = state.currentTopics,
                        isGenerating = state.isGenerating,
                        notice = state.notice,
                        error = state.error,
                        onDismissNotice = { viewModel.onAction(BrainstormAction.DismissNotice) },
                        onShareTopic = shareTopic,
                        onCopyTopic = copyTopic,
                    )
                    BrainstormTab.History -> HistoryContent(
                        sessions = state.history,
                        isLoading = state.isLoadingHistory,
                        error = state.error,
                        onShareTopic = shareTopic,
                        onCopyTopic = copyTopic,
                        notice = state.notice,
                        onDismissNotice = { viewModel.onAction(BrainstormAction.DismissNotice) },
                    )
                }
            }
        }
    }
}
