package id.walt.credentials.w3c

import kotlinx.serialization.json.*

class W3CProof(
    var type: String? = null,
    var creator: String? = null,
    var created: String? = null,
    var domain: String? = null,
    var proofPurpose: String? = null,
    var verificationMethod: String? = null,
    var jws: String? = null,
    var nonce: String? = null,
    override val properties: Map<String, Any?> = mapOf()
) : ICredentialElement {

    fun toJsonObject() = buildJsonObject {
        type?.let { put("type", it) }
        creator?.let { put("creator", it) }
        created?.let { put("created", it) }
        domain?.let { put("domain", it) }
        proofPurpose?.let { put("proofPurpose", it) }
        verificationMethod?.let { put("verificationMethod", it) }
        jws?.let { put("jws", it) }
        nonce?.let { put("nonce", it) }
        properties.let { props ->
            props.keys.forEach { key ->
                put(key, JsonConverter.toJsonElement(props[key]))
            }
        }
    }

    fun toJson() = toJsonObject().toString()

    companion object {
        val PREDEFINED_PROPERTY_KEYS = setOf(
            "type", "creator", "created", "domain", "proofPurpose", "verificationMethod", "jws", "nonce"
        )

        fun fromJsonObject(jsonObject: JsonObject): W3CProof {
            return W3CProof(
                type = jsonObject["type"]?.jsonPrimitive?.contentOrNull,
                creator = jsonObject["creator"]?.jsonPrimitive?.contentOrNull,
                created = jsonObject["created"]?.jsonPrimitive?.contentOrNull,
                domain = jsonObject["domain"]?.jsonPrimitive?.contentOrNull,
                proofPurpose = jsonObject["proofPurpose"]?.jsonPrimitive?.contentOrNull,
                verificationMethod = jsonObject["verificationMethod"]?.jsonPrimitive?.contentOrNull,
                jws = jsonObject["jws"]?.jsonPrimitive?.contentOrNull,
                nonce = jsonObject["nonce"]?.jsonPrimitive?.contentOrNull,
                properties = jsonObject.filterKeys { k -> !PREDEFINED_PROPERTY_KEYS.contains(k) }.mapValues { entry -> JsonConverter.fromJsonElement(entry.value) }
            )
        }

        fun fromJson(json: String) = fromJsonObject(kotlinx.serialization.json.Json.parseToJsonElement(json).jsonObject)
    }
}
