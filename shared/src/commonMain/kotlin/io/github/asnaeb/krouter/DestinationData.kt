package io.github.asnaeb.krouter

import androidx.navigation3.runtime.NavKey

class DestinationData<Destination : NavKey>(
    val beforeNavigate: (suspend (Destination) -> Unit)?,
    val metadata: (Destination) -> Map<String, Any>
)