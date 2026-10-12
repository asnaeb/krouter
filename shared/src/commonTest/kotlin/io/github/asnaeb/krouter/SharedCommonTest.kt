package io.github.asnaeb.krouter

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import io.github.asnaeb.krouter.SharedCommonTest.Destination
import kotlinx.serialization.Serializable
import kotlin.test.Test

class SharedCommonTest {
    @Serializable
    sealed interface Destination : NavKey

    @Serializable
    data object Dest1 : Destination
    @Serializable
    data object Dest2 : Destination

    val dest1Entry = destination<Dest1> {
        DisposableEffect(Unit) {
            println("Dest1 entered composition")
            onDispose {
                println("Dest1 exited composition")
            }
        }
    }
    val dest2Entry = destination<Dest2> {
        DisposableEffect(Unit) {
            println("Dest2 entered composition")
            onDispose {
                println("Dest2 exited composition")
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testApp() = runComposeUiTest {
        setContent {
            NavDisplay(
                backStack = rememberBackStack<Destination>(Dest1, limit = 10),
                entryProvider = rememberEntryProvider(entries = listOf(dest1Entry, dest2Entry)),
            )

            LaunchedEffect(Unit) {
                navi.navigate(Dest2)
            }
        }

        awaitIdle()
    }
}

val navi get() = Navigator<Destination>()