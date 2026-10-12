package io.github.asnaeb.krouter

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class JsonNavEntry<T>(val id: String, val data: T) {
    constructor(entry: T) : this(entry.toString(), entry)

    companion object {
        inline fun <reified T> parse(entry: NavEntry<NavKey>): T = Json
            .decodeFromString<JsonNavEntry<T>>(entry.contentKey.toString())
            .data
    }
}