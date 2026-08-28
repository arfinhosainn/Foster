package app.usenekko.home.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.usenekko.home.data.BrainstormRepository
import app.usenekko.home.domain.BrainstormDataSource
import app.usenekko.home.presentation.brainstorm.BrainstormViewModel

val LocalBrainstormDataSource = staticCompositionLocalOf<BrainstormDataSource> {
    error("BrainstormDataSource not provided")
}

val LocalBrainstormRepository = staticCompositionLocalOf<BrainstormRepository> {
    error("BrainstormRepository not provided")
}

@Composable
fun rememberBrainstormViewModel(contactId: String): BrainstormViewModel {
    val repository = LocalBrainstormRepository.current
    val contactDataSource = LocalContactDataSource.current
    return viewModel(
        key = "brainstorm-$contactId",
        factory = viewModelFactory {
            initializer { BrainstormViewModel(contactId, repository, contactDataSource) }
        },
    )
}
