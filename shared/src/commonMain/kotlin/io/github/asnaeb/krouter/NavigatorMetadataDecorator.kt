package io.github.asnaeb.krouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

class NavigatorMetadataDecorator<T : NavKey>(clz: KClass<T>) : NavEntryDecorator<T>(
    decorate = { entry ->
        val navigator = Navigator.get(clz)

        DisposableEffect(entry) {
            val metadata = entry.metadata
            navigator.withMetadata { add(metadata) }

            onDispose {
                navigator.withMetadata {
                    indexOfLast { it === metadata }.takeIf { it > -1 }?.let(::removeAt)
                }
            }
        }

        entry.Content()
    }
)

@Composable
inline fun <reified T : NavKey> rememberNavigatorMetadataDecorator(): NavEntryDecorator<T> {
    return remember { NavigatorMetadataDecorator(T::class) }
}