package io.github.asnaeb.krouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

class NavigatorMetadataDecorator<T : NavKey>(clz: KClass<T>) : NavEntryDecorator<T>(
    decorate = {
        val navigator = Navigator.get(clz)

        DisposableEffect(Unit) {
            navigator.withMetadata { add(it.metadata) }

            onDispose {
                navigator.withMetadata { remove(it.metadata) }
            }
        }

        it.Content()
    }
)

@Composable
inline fun <reified T : NavKey> rememberNavigatorMetadataDecorator(): NavEntryDecorator<T> {
    return remember { NavigatorMetadataDecorator(T::class) }
}