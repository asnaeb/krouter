package io.github.asnaeb.krouter

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.MetadataScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

interface DestinationEntry<T : NavKey> {
    val clazz: KClass<T>

    suspend fun loader(destination: T) {}

    @Composable
    fun Content(destination: T) {}


    fun metadata(destination: T): Map<String, Any> = emptyMap()
}

@DslMarker
annotation class DestinationEntryDsl

@DestinationEntryDsl
class DestinationEntryBuilder<T : NavKey>(override val clazz: KClass<T>) : DestinationEntry<T> {
    private var _loader: (suspend (T) -> Unit)? = null

    private var _content: @Composable ((T) -> Unit) = {}

    private var _metadata: (T) -> Map<String, Any> = { emptyMap() }

    fun loader(loader: suspend (T) -> Unit) {
        _loader = loader
    }

    fun content(content: @Composable (T) -> Unit) {
        _content = content
    }

    fun metadata(metadata: (T) -> Map<String, Any>) {
        _metadata = metadata
    }

    fun buildMetadata(builder: MetadataScope.(T) -> Unit) {
        _metadata = { androidx.navigation3.runtime.metadata { builder(it) } }
    }

    @Composable
    override fun Content(destination: T) {
        _content(destination)
    }

    override suspend fun loader(destination: T) {
        _loader?.invoke(destination)
    }

    override fun metadata(destination: T): Map<String, Any> {
        return _metadata(destination)
    }
}

inline fun <reified T : NavKey> destination(block: DestinationEntryBuilder<T>.() -> Unit): DestinationEntry<T> {
    return DestinationEntryBuilder(T::class).apply(block)
}

@OptIn(InternalSerializationApi::class)
fun <P : NavKey, T : P> DestinationEntry<T>.registerEntry(
    navigator: Navigator<P>,
    scope: EntryProviderScope<P>
) {
    navigator.registerDestination(clazz, ::loader)

    scope.addEntryProvider(
        clazz = clazz,
        metadata = ::metadata,
        clazzContentKey = {
            Json.encodeToString(
                JsonNavEntry.serializer(clazz.serializer()),
                JsonNavEntry(it)
            )
        },
        content = ::Content
    )
}