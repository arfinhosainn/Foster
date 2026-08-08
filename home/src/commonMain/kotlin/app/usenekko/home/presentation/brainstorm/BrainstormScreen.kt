package app.usenekko.home.presentation.brainstorm

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.usenekko.home.di.rememberBrainstormViewModel
import app.usenekko.home.presentation.brainstorm.components.BrainstormTabs
import app.usenekko.home.presentation.brainstorm.components.BrainstormTopBar
import app.usenekko.home.presentation.brainstorm.components.CurrentOutputContent
import app.usenekko.home.presentation.brainstorm.components.HistoryContent
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.gradients
import org.jetbrains.compose.resources.painterResource

@Composable
fun BrainstormScreen(
    contactId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberBrainstormViewModel(contactId)
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(BrainstormTab.CurrentOutput) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0),
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
            topBar = { BrainstormTopBar(onBack = onBack) },
            containerColor = NekkoTheme.colors.background.b0.copy(alpha = 0f),
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
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
                    )
                    BrainstormTab.History -> HistoryContent(
                        sessions = state.history,
                        isLoading = state.isLoadingHistory,
                        error = state.error,
                    )
                }
            }
        }
    }
}
