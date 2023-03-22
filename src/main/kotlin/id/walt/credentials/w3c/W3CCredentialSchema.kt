package id.walt.credentials.w3c

import kotlinx.serialization.json.*

class W3CCredentialSchema(
    var id: String,
    var type: String,
    override val properties: Map<String, Any?> = mapOf()
) : ICredentialElement {
    fun toJsonObject() = buildJsonObject {
        id.let { put("id", it) }
        type.let { put("type", it) }
        properties.let { props ->
            props.keys.forEach { key ->
                put(key, JsonConverter.toJsonElement(props[key]))
            }
        }
    }

    fun toJson() = toJsonObject().toString()

    companion object {
        val PREDEFINED_PROPERTY_KEYS = setOf(
            "id", "type"
        )

        fun fromJsonObject(jsonObject: JsonObject): W3CCredentialSchema {
            return W3CCredentialSchema(
                id = jsonObject["id"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing id property in CredentialSchema"),
                type = jsonObject["type"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing id property in CredentialSchema"),
                properties = jsonObject.filterKeys { k -> !W3CProof.PREDEFINED_PROPERTY_KEYS.contains(k) }
                    .mapValues { entry -> JsonConverter.fromJsonElement(entry.value) }
            )
        }

        fun fromJson(json: String) = fromJsonObject(Json.parseToJsonElement(json).jsonObject)
    }
}
