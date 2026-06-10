package com.nrova.sdk.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * A lossless representation of arbitrary JSON used for free-form fields
 * (release artifact extras, sales submission details, …) that the API does
 * not strictly type. Kotlin analog of Swift's `JSONValue` enum.
 */
@Serializable(with = JsonValueSerializer::class)
public sealed class JsonValue {
    public object Null : JsonValue()
    public data class Bool(public val value: Boolean) : JsonValue()
    public data class IntValue(public val value: Long) : JsonValue()
    public data class Number(public val value: Double) : JsonValue()
    public data class Text(public val value: String) : JsonValue()
    public data class Array(public val values: List<JsonValue>) : JsonValue()
    public data class Object(public val values: Map<String, JsonValue>) : JsonValue()

    /** Returns the underlying Kotlin value (`Boolean`, `Long`, `Double`, `String`, `List`, `Map` or `null`). */
    public val rawValue: Any?
        get() = when (this) {
            is Null -> null
            is Bool -> value
            is IntValue -> value
            is Number -> value
            is Text -> value
            is Array -> values.map { it.rawValue }
            is Object -> values.mapValues { it.value.rawValue }
        }

    public companion object {
        public fun fromAny(any: Any?): JsonValue = when (any) {
            null -> Null
            is Boolean -> Bool(any)
            is Int -> IntValue(any.toLong())
            is Long -> IntValue(any)
            is Float -> Number(any.toDouble())
            is Double -> Number(any)
            is String -> Text(any)
            is List<*> -> Array(any.map { fromAny(it) })
            is Map<*, *> -> Object(
                any.entries.associate { (k, v) -> k.toString() to fromAny(v) }
            )
            else -> Text(any.toString())
        }
    }
}

/**
 * Polymorphic serializer that dispatches between `JsonElement` shapes —
 * mirrors Swift's hand-written `init(from:)` over `singleValueContainer`.
 */
internal object JsonValueSerializer : KSerializer<JsonValue> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): JsonValue {
        val input = decoder as? JsonDecoder
            ?: error("JsonValue can only be deserialized from JSON")
        return fromElement(input.decodeJsonElement())
    }

    override fun serialize(encoder: Encoder, value: JsonValue) {
        val output = encoder as? JsonEncoder
            ?: error("JsonValue can only be serialized to JSON")
        output.encodeJsonElement(toElement(value))
    }

    private fun fromElement(element: JsonElement): JsonValue = when (element) {
        is JsonNull -> JsonValue.Null
        is JsonPrimitive -> when {
            element.isString -> JsonValue.Text(element.content)
            element.booleanOrNull != null -> JsonValue.Bool(element.boolean)
            element.longOrNull != null -> JsonValue.IntValue(element.longOrNull!!)
            element.doubleOrNull != null -> JsonValue.Number(element.doubleOrNull!!)
            else -> JsonValue.Text(element.contentOrNull ?: "")
        }
        is JsonArray -> JsonValue.Array(element.map { fromElement(it) })
        is JsonObject -> JsonValue.Object(element.mapValues { fromElement(it.value) })
    }

    private fun toElement(value: JsonValue): JsonElement = when (value) {
        is JsonValue.Null -> JsonNull
        is JsonValue.Bool -> JsonPrimitive(value.value)
        is JsonValue.IntValue -> JsonPrimitive(value.value)
        is JsonValue.Number -> JsonPrimitive(value.value)
        is JsonValue.Text -> JsonPrimitive(value.value)
        is JsonValue.Array -> JsonArray(value.values.map { toElement(it) })
        is JsonValue.Object -> JsonObject(value.values.mapValues { toElement(it.value) })
    }
}
