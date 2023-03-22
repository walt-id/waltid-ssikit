package id.walt.credentials.w3c

import kotlinx.serialization.json.*
import kotlin.reflect.jvm.jvmName

object JsonConverter {

    fun toJsonElement(value: Any?): JsonElement {
        return when (value) {
            is Number -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            null -> JsonNull

            is List<*> -> buildJsonArray { value.forEach { add(toJsonElement(it)) } }
            is Map<*, *> -> buildJsonObject { value.keys.forEach { put(it.toString(), toJsonElement(value[it])) } }

            is JsonElement -> value

            //else -> JsonNull
            else -> throw IllegalArgumentException("Json values can only be Number, String, Boolean, Null, List or Map, not \"${value::class.jvmName}\": toString = $value")
        }
    }

    fun fromJsonElement(element: JsonElement): Any? {
        return when (element) {
            is JsonPrimitive -> when {
                element.isString -> element.contentOrNull
                else -> element.booleanOrNull ?: element.longOrNull ?: element.doubleOrNull
            }

            is JsonArray -> element.map { fromJsonElement(it) }.toList()
            is JsonObject -> element.keys.associateWith {
                    fromJsonElement(element[it] ?: JsonNull)
                }
            else -> throw IllegalArgumentException("Invalid JSON element \"${element::class.jvmName}\": $element")
        }
    }

}
