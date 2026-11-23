package io.github.asnaeb.krouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.also
import kotlin.reflect.KClass
import kotlin.time.Duration

class Navigator<Destination : NavKey> private constructor(
    private val mutableBackStack: MutableList<Destination>,
    private val loadingDelay: Duration,
    private val onBack: MutableList<Destination>.() -> Unit
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

    fun onBack() {
        onBack.invoke(mutableBackStack)
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Destination> registerDestination(
        clz: KClass<T>,
        metadata: (T) -> Map<String, Any>,
        beforeNavigate: (suspend (T) -> Any?)?
    ) {
        require(clz !in _destinations) { "Destination ${clz}::class already registered" }

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
                            val loadingJob = launch {
                                if (loadingDelay > Duration.ZERO) {
                                    delay(loadingDelay)
                                }

                                loading += destination
                            }

                            try {
                                loaderData[destination] = loader(destination)
                            }
                            finally {
                                loadingJob.cancel()
                            }
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
        fun <Destination : NavKey> create(
            clz: KClass<Destination>,
            backStack: MutableList<Destination>,
            loadingDelay: Duration,
            onBack: MutableList<Destination>.() -> Unit
        ): Navigator<Destination> {
            require(clz !in instances) { "Navigator for ${clz.simpleName}::class already registered" }

            return instances.getOrPut(clz) {
                Navigator(
                    mutableBackStack = backStack,
                    loadingDelay = loadingDelay,
                    onBack = onBack
                )
            } as Navigator<Destination>
        }

        inline fun <reified Destination : NavKey> create(
            backStack: MutableList<Destination>,
            loadingDelay: Duration,
            noinline onBack: MutableList<Destination>.() -> Unit
        ) = create(
            clz = Destination::class,
            backStack = backStack,
            loadingDelay = loadingDelay,
            onBack = onBack
        )

        @Suppress("UNCHECKED_CAST")
        operator fun <T : NavKey> invoke(clz: KClass<T>): Navigator<T> = (instances[clz] ?: error(clz)) as Navigator<T>

        inline operator fun <reified T : NavKey> invoke(): Navigator<T> = Navigator(T::class)
    }
}

@Composable
inline fun <reified Destination : NavKey> rememberNavigator(
    backStack: MutableList<Destination>,
    loadingDelay: Duration,
    noinline onBack: MutableList<Destination>.() -> Unit
): Navigator<Destination> = remember {
    Navigator.create(
        clz = Destination::class,
        backStack = backStack,
        loadingDelay = loadingDelay,
        onBack = onBack
    )
}