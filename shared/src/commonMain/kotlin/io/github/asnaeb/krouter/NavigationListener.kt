package io.github.asnaeb.krouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation3.runtime.NavKey

@Composable
fun <T : NavKey> NavigationListener(
    navigator: Navigator<T>,
    listener: (NavigationEvent<T>) -> Boolean
) {
    DisposableEffect(Unit) {
        navigator.addNavigationListener(listener)

        onDispose {
            navigator.removeNavigationListener(listener)
        }
    }
}