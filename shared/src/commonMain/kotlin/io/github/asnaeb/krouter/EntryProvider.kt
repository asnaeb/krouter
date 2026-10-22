package io.github.asnaeb.krouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider

@Composable
inline fun <reified T : NavKey> rememberEntryProvider(
    navigator: Navigator<T> = Navigator<T>(),
    entries: List<DestinationEntry<out T>>
): (T) -> NavEntry<T> {
    return remember(entries, navigator) {
        entryProvider {
            entries.forEach {
                it.registerEntry(
                    navigator = navigator,
                    scope = this
                )
            }
        }
    }
}