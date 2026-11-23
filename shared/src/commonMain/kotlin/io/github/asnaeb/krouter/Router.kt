package io.github.asnaeb.krouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import kotlin.time.Duration

@Composable
inline fun <reified Destination : NavKey> rememberKRouter(
    destinations: List<DestinationEntry<Destination>>,
    initialEntries: List<Destination>,
    loadingDelay: Duration = Duration.ZERO,
    noinline onBack: MutableList<Destination>.() -> Unit = { removeLastOrNull() }
): KRouter<Destination> {
    val backstack = rememberBackStack<Destination>(*initialEntries.toTypedArray())

    val navigator = rememberNavigator<Destination>(
        backStack = backstack,
        loadingDelay = loadingDelay,
        onBack = onBack
    )

    val entryProvider = rememberEntryProvider<Destination>(
        navigator = navigator,
        entries = destinations
    )

    return remember {
        KRouter(
            backStack = backstack,
            navigator = navigator,
            entryProvider = entryProvider
        )
    }
}