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
import kotlinx.coroutines.withContext
import kotlin.also
import kotlin.reflect.KClass

class Navigator<Destination : NavKey> private constructor(
    private val mutableBackStack: MutableList<Destination>,
    val limit: Int
) {
    private var job: Job? = null

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val listeners = mutableListOf<(NavigationEvent<Destination>) -> Boolean>()

    val backStack: List<Destination> = mutableBackStack

    val loadingDestinations: List<Destination> field = mutableStateListOf()

    val destinations: Map<KClass<out Destination>, DestinationData<Destination>>
        field = mutableMapOf()

    val isLoading: Boolean by derivedStateOf { loadingDestinations.isNotEmpty() }

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Destination> registerDestination(
        clz: KClass<T>,
        metadata: (T) -> Map<String, Any>,
        beforeNavigate: (suspend (T) -> Unit)?
    ) {
        require(clz !in destinations) { "Destination already registered" }

        destinations[clz] = DestinationData(
            beforeNavigate = beforeNavigate,
            metadata = metadata
        ) as DestinationData<Destination>
    }

    fun canGoBack(): Boolean = mutableBackStack.size > 1

    fun onBack() {
        if (canGoBack()) {
            mutableBackStack.removeLast()
        }
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
                    destinations[destination::class]?.beforeNavigate?.let { loader ->
                        launch {
                            try {
                                loadingDestinations += destination
                                withContext(Dispatchers.Default) {
                                    loader(destination)
                                }
                            }
                            finally {
                                loadingDestinations -= destination
                            }
                        }
                    }
                }
            }

            Snapshot.withMutableSnapshot {
                mutableBackStack.clear()
                mutableBackStack.addAll(new)
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

        internal fun <Destination : NavKey> create(
            clz: KClass<Destination>,
            backStack: MutableList<Destination>,
            limit: Int
        ): Navigator<Destination> {
            return Navigator(backStack, limit).also { instances[clz] = it }
        }

        @Suppress("UNCHECKED_CAST")
        operator fun <T : NavKey> invoke(clz: KClass<T>): Navigator<T> = (instances[clz] ?: error(clz)) as Navigator<T>

        inline operator fun <reified T : NavKey> invoke(): Navigator<T> = Navigator(T::class)
    }
}