package io.github.asnaeb.krouter

import androidx.navigation3.runtime.NavKey

class NavigationEvent<T : NavKey>(from: List<T>, to: List<T>)