package io.github.asnaeb.krouter

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.test.Test

class JvmTest {
    @Serializable
    sealed interface Destination : NavKey

    @Serializable
    data object Dest1 : Destination
    @Serializable
    data object Dest2 : Destination
    @Serializable
    data object Dest3 : Destination
    @Serializable
    data object Dest4 : Destination

    object Dest3Entry : DestinationEntry<Dest3> by destination({
        content {
            DisposableEffect(Unit) {
                println("Dest3 entered composition")
                onDispose {
                    println("Dest3 exited composition")
                }
            }
        }
    })

    object Dest4Entry : DestinationEntry<Dest4> {
        override val clazz = Dest4::class

        @Composable
        override fun Content(destination: Dest4) {
            DisposableEffect(Unit) {
                println("Dest4 entered composition")
                onDispose {
                    println("Dest4 exited composition")
                }
            }
        }
    }

    val dest1Entry = destination<Dest1> {
        content {
            DisposableEffect(Unit) {
                println("${it::class.simpleName} entered composition")

                onDispose {
                    println("${it::class.simpleName} exited composition")
                }
            }
        }
    }

    val dest2Entry = destination<Dest2> {
        content {
            DisposableEffect(Unit) {
                println("Dest2 entered composition")

                onDispose {
                    println("Dest2 exited composition")
                }
            }
        }
    }

    val navigator get() = Navigator<Destination>()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testApp() = runComposeUiTest {
        setContent {
            NavDisplay(
                backStack = rememberBackStack<Destination>(Dest1, limit = 10),
                entryProvider = rememberEntryProvider(entries = listOf(dest1Entry, dest2Entry, Dest3Entry, Dest4Entry)),
            )

            LaunchedEffect(Unit) {
                navigator.navigate { add(Dest2) }
            }
        }

        awaitIdle()
    }
}