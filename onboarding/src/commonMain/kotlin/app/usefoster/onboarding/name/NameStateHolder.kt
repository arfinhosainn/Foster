package app.usefoster.onboarding.name

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun rememberNameState(): NameState {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    return NameState(firstName = firstName, lastName = lastName)
}
