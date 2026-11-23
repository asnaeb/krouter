package io.github.asnaeb.krouter

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.also
import kotlin.reflect.KClass

class Navigator<Destination : NavKey> private constructor(
    private val mutableBackStack: MutableList<Destination>,
    val limit: Int
) {
    private var job: Job? = null

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val listeners = mutableListOf<(NavigationEvent<Destination>) -> Boolean>()

    private val _destinations = mutableMapOf<KClass<out Destination>, DestinationData<Destination>>()

    private val loaderData = mutableMapOf<Destination, Any?>()

    val destinations = _destinations.values.toList()

    val backStack: List<Destination> = mutableBackStack

    val loading: List<Destination> field = mutableStateListOf()

    val isLoading: Boolean by derivedStateOf { loading.isNotEmpty() }

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Destination> registerDestination(
        clz: KClass<T>,
        metadata: (T) -> Map<String, Any>,
        beforeNavigate: (suspend (T) -> Any?)?
    ) {
        require(clz !in _destinations) { "Destination already registered" }

        _destinations[clz] = DestinationData(
            clazz = clz,
            beforeNavigate = beforeNavigate,
            metadata = metadata
        ) as DestinationData<Destination>
    }

    internal fun clearLoaderData(destination: Destination) {
        loaderData.remove(destination)
    }

    internal fun getLoaderData(destination: Destination) = loaderData[destination]

    fun canGoBack(): Boolean = mutableBackStack.size > 1

    fun onBack() {
        if (canGoBack()) {
            mutableBackStack.removeLast()
        }
    }

    fun metadata(destination: Destination): Map<String, Any> {
        return _destinations[destination::class]?.metadata(destination) ?: emptyMap()
    }

    inline fun <reified T : Destination> isLoading(destination: T): Boolean {
        return destination in loading
    }

    fun addNavigationListener(listener: (NavigationEvent<Destination>) -> Boolean) {
        listeners.add(listener)
    }

    fun removeNavigationListener(listener: (NavigationEvent<Destination>) -> Boolean) {
        listeners.remove(listener)
    }

    fun navigate(handler: MutableList<Destination>.() -> Unit) {
        val new: List<Destination> = mutableBackStack.toMutableList().apply(handler)

        listeners.forEach { listener ->
            val block: Boolean = listener(NavigationEvent(from = mutableBackStack, to = new))

            if (block) {
                return@navigate
            }
        }

        val newDestinations: Set<Destination> = new.subtract(mutableBackStack.toSet())

        job?.cancel()

        job = scope.launch {
            coroutineScope {
                newDestinations.forEach { destination ->
                    _destinations[destination::class]?.beforeNavigate?.let { loader ->
                        launch {
                            loading += destination
                            loaderData[destination] = loader(destination)
                        }
                    }
                }
            }

            Snapshot.withMutableSnapshot {
                mutableBackStack.clear()
                mutableBackStack.addAll(new)
            }

            Snapshot.withMutableSnapshot {
                newDestinations.forEach(loading::minusAssign)
            }
        }
    }

    companion object {
        private val instances: MutableMap<KClass<out NavKey>, Navigator<out NavKey>> = mutableMapOf()

        private fun error(clz: KClass<out NavKey>): Nothing = error("""
            Navigator for ${clz.simpleName} not found. 
            Before using Navigator<${clz.simpleName}>() or Navigator(${clz.simpleName}::class) make sure to call rememberBackStack<${clz.simpleName}>() once
        """.replace("\n", " ").trimIndent()
        )

        @Suppress("UNCHECKED_CAST")
        internal fun <Destination : NavKey> create(
            clz: KClass<Destination>,
            backStack: MutableList<Destination>,
            limit: Int
        ): Navigator<Destination> {
            return instances.getOrPut(clz) { Navigator(backStack, limit) } as Navigator<Destination>
        }

        @Suppress("UNCHECKED_CAST")
        operator fun <T : NavKey> invoke(clz: KClass<T>): Navigator<T> = (instances[clz] ?: error(clz)) as Navigator<T>

        inline operator fun <reified T : NavKey> invoke(): Navigator<T> = Navigator(T::class)
    }
}