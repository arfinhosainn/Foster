package app.usefoster.home.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.usefoster.home.data.BrainstormRepository
import app.usefoster.home.domain.BrainstormDataSource
import app.usefoster.home.presentation.brainstorm.BrainstormViewModel

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
