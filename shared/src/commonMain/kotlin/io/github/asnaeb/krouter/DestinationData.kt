package io.github.asnaeb.krouter

import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

class DestinationData<Destination : NavKey>(
    internal val beforeNavigate: (suspend (Destination) -> Any?)?,
    val metadata: (Destination) -> Map<String, Any>,
    val clazz: KClass<Destination>
)