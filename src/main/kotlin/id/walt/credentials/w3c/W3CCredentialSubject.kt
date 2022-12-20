package id.walt.credentials.w3c

import kotlinx.serialization.json.*

open class W3CCredentialSubject(var id: String? = null, override val properties: Map<String, Any?> = mapOf()) :
    ICredentialElement {

    fun toJsonObject() = buildJsonObject {
        id?.let { put("id", it) }
        properties.let { props ->
            props.keys.forEach { key ->
                put(key, JsonConverter.toJsonElement(props[key]))
            }
        }
    }

    fun toJson() = toJsonObject().toString()

    companion object {
        fun fromJson(json: String): W3CCredentialSubject {
            return fromJsonObject(Json.parseToJsonElement(json).jsonObject)
        }

        fun fromJsonObject(jsonObject: JsonObject): W3CCredentialSubject {
            return W3CCredentialSubject(
                jsonObject["id"]?.jsonPrimitive?.contentOrNull,
                jsonObject.filterKeys { k -> k != "id" }.mapValues { entry -> JsonConverter.fromJsonElement(entry.value) }
            )
        }
    }
}
