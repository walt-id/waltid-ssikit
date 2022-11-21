package id.walt.credentials.w3c

import kotlinx.serialization.json.*

object JsonConverter {

    fun toJsonElement(value: Any?): JsonElement {
        return when(value) {
            is Number -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is List<*> -> buildJsonArray {
                value.forEach { add(toJsonElement(it)) }
            }
            is Map<*, *> -> buildJsonObject {
                value.keys.forEach { key ->
                    put(key.toString(), toJsonElement(value[key]))
                }
            }
            is JsonElement -> value
            null -> JsonNull
            else -> throw Exception("Json values can only be Number, String, List or Map")
        }
    }

    fun fromJsonElement(element: JsonElement): Any? {
        return when(element) {
            is JsonPrimitive -> if(element.isString) {
                element.contentOrNull
            } else {
                element.booleanOrNull ?: element.longOrNull ?: element.doubleOrNull
            }
            is JsonArray -> element.map { fromJsonElement(it) }.toList()
            is JsonObject -> element.keys.associateWith {
                fromJsonElement(element[it] ?: JsonNull)
            }
        }
    }

}
