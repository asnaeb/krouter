package io.github.asnaeb.krouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

interface DestinationEntry<Destination : NavKey> {
    val clazz: KClass<Destination>

    val beforeNavigate: (suspend (Destination) -> Any?)?

    val metadata: (Destination) -> Map<String, Any>

    val content: @Composable (Destination, data: Any?) -> Unit
}

inline fun <reified Destination : NavKey> DestinationEntry(
    noinline metadata: (Destination) -> Map<String, Any> = { emptyMap() },
    noinline content: @Composable (Destination) -> Unit = {}
) = object : DestinationEntry<Destination> {
    override val clazz: KClass<Destination> = Destination::class

    override val beforeNavigate: (suspend (Destination) -> Any?)? = null

    override val metadata = metadata

    override val content = @Composable { destination: Destination, _: Any? ->
        content(destination)
    }
}

inline fun <reified Destination : NavKey, Data> DestinationEntry(
    noinline metadata: (Destination) -> Map<String, Any> = { emptyMap() },
    noinline loader: suspend (Destination) -> Data,
    noinline content: @Composable (Destination, Data) -> Unit = { _, _ -> }
) = object : DestinationEntry<Destination> {
    override val clazz: KClass<Destination> = Destination::class

    override val metadata = metadata

    override val beforeNavigate: suspend (Destination) -> Data = { loader(it) }

    override val content = @Composable { destination: Destination, data: Any? ->
        @Suppress("UNCHECKED_CAST")
        content(destination, data as Data)
    }
}

inline fun <reified Destination : NavKey> DestinationEntry(
    noinline metadata: (Destination) -> Map<String, Any> = { emptyMap() },
    noinline loader: suspend (Destination) -> Unit,
    noinline content: @Composable (Destination) -> Unit = { }
): DestinationEntry<Destination> = DestinationEntry<Destination, Unit>(
    metadata = metadata,
    loader = loader,
    content = { destination, _ ->  content(destination) }
)

@OptIn(InternalSerializationApi::class)
fun <P : NavKey, T : P> DestinationEntry<T>.registerEntry(
    navigator: Navigator<P>,
    scope: EntryProviderScope<P>
) {
    navigator.registerDestination(clazz, metadata, beforeNavigate)

    scope.addEntryProvider(
        clazz = clazz,
        metadata = metadata,
        clazzContentKey = {
            Json.encodeToString(
                JsonNavEntry.serializer(clazz.serializer()),
                JsonNavEntry(it)
            )
        },
        content = { destination: T ->
            content(destination, navigator.getLoaderData(destination))

            DisposableEffect(destination) {
                onDispose {
                    navigator.clearLoaderData(destination)
                }
            }
        }
    )
}