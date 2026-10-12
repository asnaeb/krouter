package io.github.asnaeb.krouter

import androidx.navigation3.runtime.NavKey

internal class DestinationData<Destination : NavKey>(
    val beforeNavigate: (suspend (Destination) -> Unit)?,
)