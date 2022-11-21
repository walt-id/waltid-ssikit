package id.walt.credentials.w3c

import id.walt.credentials.w3c.builder.CredentialFactory
import id.walt.vclib.model.CredentialSchema
import id.walt.vclib.model.Proof
import id.walt.vclib.model.VerifiableCredential
import kotlinx.serialization.json.*

open class W3CCredential internal constructor (
    override var type: List<String> = listOf("VerifiableCredential"),
    var context: List<W3CContext> = listOf(W3CContext("https://www.w3.org/2018/credentials/v1", false)),
    override var id: String? = null,
    var w3cIssuer: W3CIssuer? = null,
    override var issuanceDate: String? = null,
    override var issued: String? = null,
    override var validFrom: String? = null,
    override var expirationDate: String? = null,
    var w3cProof: W3CProof? = null,
    var w3cCredentialSchema: W3CCredentialSchema? = null,
    var credentialSubject: W3CCredentialSubject? = null,
    override val properties: Map<String, Any?> = mapOf()
): VerifiableCredential(), ICredentialElement {

    internal constructor(jsonObject: JsonObject) : this(
        type = jsonObject["type"]?.jsonArray?.map { it.jsonPrimitive.content }?.toList() ?: listOf("VerifiableCredential"),
        context = jsonObject["@context"]?.let { ctx ->
            when(ctx) {
                is JsonArray -> ctx.map { W3CContext.fromJsonElement(it) }.toList()
                else -> listOf(W3CContext.fromJsonElement(ctx))
            }
        } ?: listOf(W3CContext("https://www.w3.org/2018/credentials/v1", false)),
        id = jsonObject["id"]?.jsonPrimitive?.contentOrNull,
        w3cIssuer = jsonObject["issuer"]?.let { W3CIssuer.fromJsonElement(it) },
        issuanceDate = jsonObject["issuanceDate"]?.jsonPrimitive?.contentOrNull,
        issued = jsonObject["issued"]?.jsonPrimitive?.contentOrNull,
        validFrom = jsonObject["validFrom"]?.jsonPrimitive?.contentOrNull,
        expirationDate = jsonObject["expirationDate"]?.jsonPrimitive?.contentOrNull,
        w3cProof = jsonObject["proof"]?.let { it as? JsonObject }?.let { W3CProof.fromJsonObject(it) },
        w3cCredentialSchema = jsonObject["credentialSchema"]?.let { it as? JsonObject }?.let { W3CCredentialSchema.fromJsonObject(it) },
        credentialSubject = jsonObject["credentialSubject"]?.let { it as? JsonObject }?.let { W3CCredentialSubject.fromJsonObject(it) },
        properties = jsonObject.filterKeys { k -> !PREDEFINED_PROPERTY_KEYS.contains(k) }.mapValues { entry -> JsonConverter.fromJsonElement(entry.value) }
    )

    override var issuer: String?
        get() = w3cIssuer?.id
        set(value) {
            w3cIssuer = value?.let { v -> w3cIssuer?.apply { id = v } ?: W3CIssuer(v) }
        }

    override var subject: String?
        get() = credentialSubject?.id
        set(value) {
            credentialSubject = value?.let { v -> credentialSubject?.apply { id = v } ?: W3CCredentialSubject(v) }
        }

    override var proof: Proof?
        get() = w3cProof
        set(value) {
            if(value is W3CProof) {
                w3cProof = value
            } else {
                w3cProof = value?.let { W3CProof(it) }
            }
        }

    override val credentialSchema: CredentialSchema?
        get() = w3cCredentialSchema

    override fun newId(id: String) = "${type.last()}#${id}"

    fun toJsonObject() = buildJsonObject {
        put("type", JsonConverter.toJsonElement(type))
        put("@context",buildJsonArray {
            context.forEach { add(it.toJsonElement()) }
        })
        id?.let { put("id", JsonConverter.toJsonElement(it)) }
        w3cIssuer?.let { put("issuer", it.toJsonElement()) }
        issuanceDate?.let { put("issuanceDate", JsonConverter.toJsonElement(it)) }
        issued?.let { put("issued", JsonConverter.toJsonElement(it)) }
        validFrom?.let { put("validFrom", JsonConverter.toJsonElement(it)) }
        expirationDate?.let { put("expirationDate", JsonConverter.toJsonElement(it)) }
        w3cProof?.let { put("proof", it.toJsonObject()) }
        w3cCredentialSchema?.let { put("credentialSchema", it.toJsonObject()) }
        credentialSubject?.let { put("credentialSubject", it.toJsonObject()) }
        properties.keys.forEach { key ->
            put(key, JsonConverter.toJsonElement(properties[key]))
        }
    }

    fun toJson() = toJsonObject().toString()

    companion object : CredentialFactory<W3CCredential> {
        val PREDEFINED_PROPERTY_KEYS = setOf(
            "type",
            "@context",
            "id",
            "issuer",
            "issued",
            "issuanceDate",
            "validFrom",
            "expirationDate",
            "proof",
            "credentialSchema",
            "credentialSubject"
        )

        override fun fromJsonObject(jsonObject: JsonObject): W3CCredential {
            return W3CCredential(jsonObject)
        }
    }
}
