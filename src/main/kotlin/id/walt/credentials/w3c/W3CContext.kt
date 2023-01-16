package id.walt.credentials.w3c

import kotlinx.serialization.json.*

class W3CContext(
    var uri: String?,
    val _isObject: Boolean,
    override val properties: Map<String, Any?> = mapOf()
) : ICredentialElement {
    constructor(uri: String) : this(uri, false)
    constructor(properties: Map<String, Any?>) : this(null, true, properties)

    fun toJsonElement(): JsonElement {
        return if (_isObject) {
            buildJsonObject {
                uri?.let { put("uri", it) }
                properties.let { props ->
                    props.keys.forEach { key ->
                        put(key, JsonConverter.toJsonElement(props[key]))
                    }
                }
            }
        } else {
            uri?.let { JsonPrimitive(it) } ?: JsonNull
        }
    }

    fun toJson() = toJsonElement().toString()

    companion object {
        fun fromJson(json: String): W3CContext {
            return fromJsonElement(Json.parseToJsonElement(json))
        }

        fun fromJsonElement(jsonElement: JsonElement): W3CContext {
            return if (jsonElement is JsonObject) {
                W3CContext(
                    jsonElement["uri"]?.jsonPrimitive?.contentOrNull,
                    _isObject = true,
                    jsonElement.filterKeys { k -> k != "uri" }
                        .mapValues { entry -> JsonConverter.fromJsonElement(entry.value) }
                )
            } else {
                W3CContext(uri = jsonElement.jsonPrimitive.contentOrNull, _isObject = false)
            }
        }
    }
}
