package io.github.asnaeb.krouter

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.also
import kotlin.reflect.KClass

class Navigator<Destination : NavKey> private constructor(
    val backStack: MutableList<Destination>,
    val limit: Int
) {
    private var job: Job? = null

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)

    private val data: MutableMap<KClass<out Destination>, DestinationData<Destination>> = mutableMapOf()

    private val listeners: MutableList<(NavigationEvent<Destination>) -> Boolean> = mutableListOf()

    val loadingDestinations: StateFlow<List<Destination>> field = MutableStateFlow(emptyList())

    val activeDestination: Destination by derivedStateOf { backStack.last() }

    val isLoading: StateFlow<Boolean> = loadingDestinations.map { it.isNotEmpty() }
        .stateIn(scope, SharingStarted.Eagerly, false)

    @Suppress("UNCHECKED_CAST")
    internal fun <T : Destination> registerDestination(
        clz: KClass<T>,
        beforeNavigate: (suspend (T) -> Unit)?
    ) {
        data[clz] = DestinationData(beforeNavigate = beforeNavigate) as DestinationData<Destination>
    }

    fun canGoBack(): Boolean = backStack.size > 1

    fun onBack() {
        if (canGoBack()) {
            backStack.removeLast()
        }
    }

    fun addNavigationListener(listener: (NavigationEvent<Destination>) -> Boolean) {
        listeners.add(listener)
    }

    fun removeNavigationListener(listener: (NavigationEvent<Destination>) -> Boolean) {
        listeners.remove(listener)
    }

    fun navigate(handler: MutableList<Destination>.() -> Unit) {
        val new: List<Destination> = backStack.toMutableList().apply(handler)

        listeners.forEach { listener ->
            val block: Boolean = listener(NavigationEvent(from = backStack, to = new))

            if (block) {
                return@navigate
            }
        }

        val newDestinations: List<Destination> = new.filter { it !in backStack }

        scope.launch {
            coroutineScope {
                newDestinations.forEach { destination ->
                    data[destination::class]?.beforeNavigate?.let { loader ->
                        launch {
                            try {
                                loadingDestinations.update { it + destination }
                                loader(destination)
                            }
                            finally {
                                loadingDestinations.update { it - destination }
                            }
                        }
                    }
                }
            }

            Snapshot.withMutableSnapshot {
                backStack.clear()
                backStack.addAll(new)
            }
        }
    }

    fun navigate(to: Destination, replace: Boolean = false) {
        if (activeDestination != to) {
            job?.cancel()

            job = scope.launch {
                val prev: MutableList<Destination> = backStack.toMutableList()
                val new: List<Destination> = prev.apply {
                    if (replace) {
                        set(lastIndex, to)
                    }
                    else {
                        if (size + 1 > limit) {
                            removeFirst()
                        }

                        add(to)
                    }
                }

                listeners.forEach { listener ->
                    val block: Boolean = listener(NavigationEvent(from = prev, to = new))

                    if (block) {
                        return@launch
                    }
                }

                val targetDestinationData: DestinationData<Destination>? = data[to::class]

                try {
                    targetDestinationData?.beforeNavigate?.let { beforeNavigate ->
                        loadingDestinations.update { it + to }
                        beforeNavigate(to)
                    }
                }
                catch (e: Exception) {
                    if (e is CancellationException) {
                        throw e
                    }

                    e.printStackTrace()
                    return@launch
                }
                finally {
                    loadingDestinations.update { it - to }
                }

                Snapshot.withMutableSnapshot {
                    backStack.clear()
                    backStack.addAll(new)
                }
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