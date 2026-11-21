package io.github.asnaeb.krouter

import androidx.navigation3.runtime.NavKey

inline fun <reified T : NavKey> T.metadata(): Map<String, Any> {
    return Navigator<T>().destinations[T::class]?.metadata(this) ?: emptyMap()
}

inline fun <reified T : NavKey> T.isLoading(): Boolean {
    return this in Navigator<T>().loadingDestinations
}