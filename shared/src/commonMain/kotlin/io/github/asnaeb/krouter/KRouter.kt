package io.github.asnaeb.krouter

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

class KRouter<Destination : NavKey>(
    val backStack: List<Destination>,
    val navigator: Navigator<Destination>,
    val entryProvider: (Destination) -> NavEntry<Destination>
)