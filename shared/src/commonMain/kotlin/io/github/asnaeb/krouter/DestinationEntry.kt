package io.github.asnaeb.krouter

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface DestinationEntry<T : NavKey> {
    val clz: KClass<T>
    val type: KType
    val loader: (suspend (T) -> Unit)?
    val content: @Composable (T) -> Unit
    val metadata: (T) -> Map<String, Any>
}

inline fun <reified T : NavKey> destination(
    metadata: Map<String, Any> = emptyMap(),
    noinline loader: (suspend (T) -> Unit)? = null,
    noinline content: @Composable (T) -> Unit
) = destination(
    metadata = { metadata },
    loader = loader,
    content = content
)

inline fun <reified T : NavKey> destination(
    noinline metadata: (T) -> Map<String, Any>,
    noinline loader: (suspend (T) -> Unit)? = null,
    noinline content: @Composable (T) -> Unit
) = object : DestinationEntry<T> {
    override val clz: KClass<T> = T::class
    override val type: KType = typeOf<T>()
    override val loader: (suspend (T) -> Unit)? = loader
    override val metadata: (T) -> Map<String, Any> = metadata
    override val content: @Composable (T) -> Unit = content
}

@OptIn(InternalSerializationApi::class)
fun <P : NavKey, T : P> DestinationEntry<T>.registerEntry(navigator: Navigator<P>, scope: EntryProviderScope<P>) {
    loader?.let { loader ->
        navigator.registerDestination(clz) { loader(it) }
    }

    scope.addEntryProvider(
        clazz = clz,
        metadata = metadata,
        clazzContentKey = { Json.encodeToString(JsonNavEntry.serializer(clz.serializer()), JsonNavEntry(it)) },
        content = content
    )
}