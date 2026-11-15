package app.shiguang.imaging

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Stable JSON encoding used for custom presets persisted in DataStore. */
object LookJson {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun encode(params: LookParams): String =
        json.encodeToString(LookParams.serializer(), params)

    fun decode(text: String): LookParams? =
        runCatching { json.decodeFromString(LookParams.serializer(), text) }.getOrNull()

    fun encodeList(params: List<LookParams>): String =
        json.encodeToString(ListSerializer(LookParams.serializer()), params)

    fun decodeList(text: String): List<LookParams> =
        runCatching {
            json.decodeFromString(ListSerializer(LookParams.serializer()), text)
        }.getOrDefault(emptyList())
}
