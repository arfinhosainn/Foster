package app.usenekko.home.presentation.brainstorm.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.usenekko.home.domain.BrainstormTopic
import app.usenekko.home.presentation.brainstorm.BrainstormTab
import app.usenekko.theme.NekkoTheme
import nekko.home.generated.resources.Res
import nekko.home.generated.resources.ic_back
import org.jetbrains.compose.resources.vectorResource

@Composable
fun BrainstormTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) = CenterAlignedTopAppBar(
    modifier = modifier,
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = NekkoTheme.colors.background.b0,
        scrolledContainerColor = NekkoTheme.colors.background.b0,
    ),
    title = {
        Text(
            "Brainstorm",
            style = NekkoTheme.typography.heading1Bold,
            color = NekkoTheme.colors.text.primary,
        )
    },
    navigationIcon = {
        FilledIconButton(
            modifier = Modifier.size(58.dp),
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(containerColor = NekkoTheme.colors.fill.tertiary),
        ) {
            Image(imageVector = vectorResource(Res.drawable.ic_back), contentDescription = "Back")
        }
    },
)

@Composable
fun BrainstormTabs(
    selected: BrainstormTab,
    onSelect: (BrainstormTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(BrainstormTab.CurrentOutput to "Current Output", BrainstormTab.History to "History")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (tab, label) ->
            val isSelected = tab == selected
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) NekkoTheme.colors.green.active else NekkoTheme.colors.fill.quaternary,
                onClick = { onSelect(tab) },
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    color = if (isSelected) Color.White else NekkoTheme.colors.text.secondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun TopicCard(
    topic: BrainstormTopic,
    index: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(NekkoTheme.colors.fill.quaternary)
            .padding(20.dp),
    ) {
        Text(
            text = "${index + 1}.  ${topic.title}",
            style = NekkoTheme.typography.heading4Semibold,
            color = NekkoTheme.colors.text.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = topic.description.orEmpty(),
            style = NekkoTheme.typography.bodyMedium,
            color = NekkoTheme.colors.text.secondary,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
}
