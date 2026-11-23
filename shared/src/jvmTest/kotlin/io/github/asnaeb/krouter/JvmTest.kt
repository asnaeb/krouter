package io.github.asnaeb.krouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable
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

    object Dest3Entry : DestinationEntry<Dest3> by DestinationEntry(
        loader = { "hello" },
        content = { dest3, string ->
            DisposableEffect(Unit) {
                println("Dest3 entered composition")
                onDispose {
                    println("Dest3 exited composition")
                }
            }
        }
    )

    val dest1Entry = DestinationEntry<Dest1> {
        DisposableEffect(Unit) {
            println("${it::class.simpleName} entered composition")

            onDispose {
                println("${it::class.simpleName} exited composition")
            }
        }
    }

    val dest2Entry = DestinationEntry<Dest2> {
        DisposableEffect(Unit) {
            println("Dest2 entered composition")

            onDispose {
                println("Dest2 exited composition")
            }
        }
    }

    val navigator get() = Navigator<Destination>()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testApp() = runComposeUiTest {
        setContent {
            var state by remember { mutableStateOf(false) }

            NavDisplay(
                backStack = rememberBackStack<Destination>(Dest1, limit = 10),
                entryProvider = rememberEntryProvider(entries = listOf(dest1Entry, dest2Entry, Dest3Entry)),
            )

            LaunchedEffect(Unit) {
                navigator.navigate { add(Dest2) }
            }
        }

        awaitIdle()
    }
}