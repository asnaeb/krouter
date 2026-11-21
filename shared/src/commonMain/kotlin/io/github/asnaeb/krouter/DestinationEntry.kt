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

    fun metadata(destination: Destination): Map<String, Any> = emptyMap()
}

interface PlainDestinationEntry<Destination : NavKey> : DestinationEntry<Destination> {
    @Composable
    fun Content(destination: Destination) {}
}

interface DataDestinationEntry<Destination : NavKey, Data> : DestinationEntry<Destination> {
    suspend fun beforeNavigate(destination: Destination): Data

    @Composable
    fun Content(destination: Destination, data: Data) {}
}

inline fun <reified Destination : NavKey> DestinationEntry(
    noinline metadata: (Destination) -> Map<String, Any> = { emptyMap() },
    noinline content: @Composable (Destination) -> Unit = {}
): DestinationEntry<Destination> = object : PlainDestinationEntry<Destination> {
    override val clazz: KClass<Destination> = Destination::class

    override fun metadata(destination: Destination): Map<String, Any> {
        return metadata(destination)
    }

    @Composable
    override fun Content(destination: Destination) {
        content(destination)
    }
}

inline fun <reified Destination : NavKey, Data> DestinationEntry(
    noinline metadata: (Destination) -> Map<String, Any> = { emptyMap() },
    noinline loader: (Destination) -> Data,
    noinline content: @Composable (Destination, Data) -> Unit = { _, _ -> }
): DestinationEntry<Destination> = object : DataDestinationEntry<Destination, Data> {
    override val clazz: KClass<Destination> = Destination::class

    override fun metadata(destination: Destination): Map<String, Any> {
        return metadata(destination)
    }

    override suspend fun beforeNavigate(destination: Destination): Data = loader(destination)

    @Composable
    override fun Content(destination: Destination, data: Data) {
        content(destination, data)
    }
}

inline fun <reified Destination : NavKey> DestinationEntry(
    noinline metadata: (Destination) -> Map<String, Any> = { emptyMap() },
    noinline loader: (Destination) -> Unit,
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
    val loader = if (this is DataDestinationEntry<T, *>) {
        ::beforeNavigate
    }
    else {
        null
    }

    navigator.registerDestination(clazz, ::metadata, loader)

    scope.addEntryProvider(
        clazz = clazz,
        metadata = ::metadata,
        clazzContentKey = {
            Json.encodeToString(
                JsonNavEntry.serializer(clazz.serializer()),
                JsonNavEntry(it)
            )
        },
        content = when (this) {
            is DataDestinationEntry<T, *> -> {
                @Composable { destination: T ->
                    @Suppress("UNCHECKED_CAST")
                    (::Content as? @Composable (T, Any?) -> Unit)?.invoke(
                        destination,
                        navigator.getLoaderData(destination)
                    )

                    DisposableEffect(destination) {
                        onDispose {
                            navigator.clearLoaderData(destination)
                        }
                    }
                }
            }

            is PlainDestinationEntry<T> -> {
                @Composable { destination: T -> Content(destination) }
            }

            else -> { @Composable { } }
        }
    )
}