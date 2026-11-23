package io.github.asnaeb.krouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
@Composable
fun <T : NavKey> rememberBackStack(
    destination: KClass<T>,
    vararg initialEntries: T
): NavBackStack<T> {
    val configuration = remember {
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclassesOfSealed(destination.serializer())
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val backStack: NavBackStack<T> = rememberNavBackStack(
        configuration = configuration,
        *initialEntries
    ) as NavBackStack<T>

    return backStack
}

@Composable
inline fun <reified T : NavKey> rememberBackStack(
    vararg initialEntries: T
): NavBackStack<T> =
    rememberBackStack(destination = T::class, initialEntries = initialEntries)