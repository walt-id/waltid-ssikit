package id.walt.credentials.w3c

import kotlinx.serialization.json.*

class W3CIssuer(
    var id: String,
    val _isObject: Boolean,
    override val properties: Map<String, Any?> = mapOf(),
) : ICredentialElement {
    constructor(id: String) : this(id, false)
    constructor(id: String, properties: Map<String, Any?>) : this(id, true, properties)

    fun toJsonElement(): JsonElement {
        return if (_isObject) {
            buildJsonObject {
                id.let { put("id", it) }
                properties.let { props ->
                    props.keys.forEach { key ->
                        put(key, JsonConverter.toJsonElement(props[key]))
                    }
                }
            }
        } else {
            JsonPrimitive(id)
        }
    }

    fun toJson() = toJsonElement().toString()

    companion object {
        fun fromJson(json: String): W3CIssuer {
            return fromJsonElement(Json.parseToJsonElement(json))
        }

        fun fromJsonElement(jsonElement: JsonElement): W3CIssuer {
            return if (jsonElement is JsonObject) {
                W3CIssuer(
                    jsonElement["id"]!!.jsonPrimitive.content,
                    jsonElement.filterKeys { k -> k != "id" }.mapValues { entry -> JsonConverter.fromJsonElement(entry.value) }
                )
            } else {
                W3CIssuer(jsonElement.jsonPrimitive.content)
            }
        }
    }
}
