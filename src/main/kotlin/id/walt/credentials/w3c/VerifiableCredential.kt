package id.walt.credentials.w3c

import com.nimbusds.jwt.SignedJWT
import id.walt.credentials.selectiveDisclosure.SDField
import id.walt.credentials.w3c.builder.CredentialFactory
import id.walt.services.sdjwt.SDJwtService
import kotlinx.serialization.json.*

open class VerifiableCredential internal constructor(
    var type: List<String> = listOf("VerifiableCredential"),
    var context: List<W3CContext> = listOf(W3CContext("https://www.w3.org/2018/credentials/v1", false)),
    var id: String? = null,
    var issuer: W3CIssuer? = null,
    var issuanceDate: String? = null,
    var issued: String? = null,
    var validFrom: String? = null,
    var expirationDate: String? = null,
    var proof: W3CProof? = null,
    var jwt: String? = null,
    var credentialSchema: W3CCredentialSchema? = null,
    var credentialSubject: W3CCredentialSubject? = null,
    override val properties: Map<String, Any?> = mapOf(),
    var selectiveDisclosure: Map<String, SDField>? = null
) : ICredentialElement {

    internal constructor(jsonObject: JsonObject) : this(
        type = jsonObject["type"]?.jsonArray?.map { it.jsonPrimitive.content }?.toList() ?: listOf("VerifiableCredential"),
        context = jsonObject["@context"]?.let { ctx ->
            when (ctx) {
                is JsonArray -> ctx.map { W3CContext.fromJsonElement(it) }.toList()
                else -> listOf(W3CContext.fromJsonElement(ctx))
            }
        } ?: listOf(W3CContext("https://www.w3.org/2018/credentials/v1", false)),
        id = jsonObject["id"]?.jsonPrimitive?.contentOrNull,
        issuer = jsonObject["issuer"]?.let { W3CIssuer.fromJsonElement(it) },
        issuanceDate = jsonObject["issuanceDate"]?.jsonPrimitive?.contentOrNull,
        issued = jsonObject["issued"]?.jsonPrimitive?.contentOrNull,
        validFrom = jsonObject["validFrom"]?.jsonPrimitive?.contentOrNull,
        expirationDate = jsonObject["expirationDate"]?.jsonPrimitive?.contentOrNull,
        proof = jsonObject["proof"]?.let { it as? JsonObject }?.let { W3CProof.fromJsonObject(it) },
        credentialSchema = jsonObject["credentialSchema"]?.let { it as? JsonObject }
            ?.let { W3CCredentialSchema.fromJsonObject(it) },
        credentialSubject = jsonObject["credentialSubject"]?.let { it as? JsonObject }
            ?.let { W3CCredentialSubject.fromJsonObject(it) },
        properties = jsonObject.filterKeys { k -> !PREDEFINED_PROPERTY_KEYS.contains(k) }
            .mapValues { entry -> JsonConverter.fromJsonElement(entry.value) }
    )

    open val issuerId: String?
        get() = issuer?.id

    open val subjectId: String?
        get() = credentialSubject?.id

    open val challenge
        get() = when (this.jwt) {
            null -> this.proof?.nonce
            else -> SignedJWT.parse(this.jwt).jwtClaimsSet.getStringClaim("nonce")
        }

    fun toJsonObject() = buildJsonObject {
        put("type", JsonConverter.toJsonElement(type))
        put("@context", buildJsonArray {
            context.forEach { add(it.toJsonElement()) }
        })
        id?.let { put("id", JsonConverter.toJsonElement(it)) }
        issuer?.let { put("issuer", it.toJsonElement()) }
        issuanceDate?.let { put("issuanceDate", JsonConverter.toJsonElement(it)) }
        issued?.let { put("issued", JsonConverter.toJsonElement(it)) }
        validFrom?.let { put("validFrom", JsonConverter.toJsonElement(it)) }
        expirationDate?.let { put("expirationDate", JsonConverter.toJsonElement(it)) }
        proof?.let { put("proof", it.toJsonObject()) }
        credentialSchema?.let { put("credentialSchema", it.toJsonObject()) }
        credentialSubject?.let { put("credentialSubject", it.toJsonObject()) }
        properties.keys.forEach { key ->
            put(key, JsonConverter.toJsonElement(properties[key]))
        }
    }

    fun toJson(): String {
        return toJsonObject().toString()
    }

    fun toJsonElement() = jwt?.let { JsonPrimitive(it) } ?: toJsonObject()
    override fun toString(): String {
        return jwt ?: toJson()
    }

    fun encode() = toString()

    companion object : CredentialFactory<VerifiableCredential> {
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

        override fun fromJsonObject(jsonObject: JsonObject): VerifiableCredential {
            return VerifiableCredential(jsonObject)
        }

        private const val JWT_PATTERN = "(^[A-Za-z0-9-_]*\\.[A-Za-z0-9-_]*\\.[A-Za-z0-9-_]*\$)"
        private const val JWT_VC_CLAIM = "vc"
        private const val JWT_VP_CLAIM = "vp"
        private const val SD_JWT_PATTERN = "^([A-Za-z0-9-_]+)\\.([A-Za-z0-9-_]+)\\.([A-Za-z0-9-_]+)(~([A-Za-z0-9-_]+))*(~(([A-Za-z0-9-_]+)\\.([A-Za-z0-9-_]+)\\.([A-Za-z0-9-_]+))?)?\$"

        fun isJWT(data: String) = Regex(JWT_PATTERN).matches(data)
        fun isSDJwt(data: String) = Regex(SD_JWT_PATTERN).matches(data)

        private val possibleClaimKeys = listOf(JWT_VP_CLAIM, JWT_VC_CLAIM)

        private fun fromJwt(jwt: String): VerifiableCredential {
            val claims = SignedJWT.parse(jwt).jwtClaimsSet.claims

            val claimKey = possibleClaimKeys.first { it in claims }

            val claim = claims[claimKey]
            return fromJsonObject(JsonConverter.toJsonElement(claim).jsonObject).apply {
                this.jwt = jwt
            }
        }

        private fun fromSdJwt(combinedSdJwt: String): VerifiableCredential {
            val resolvedObject = SDJwtService.getService().parsePayload(combinedSdJwt)
            val claimKey = possibleClaimKeys.first { it in resolvedObject.keys }
            return fromJsonObject(resolvedObject[claimKey]!!.jsonObject).apply {
                this.jwt = combinedSdJwt
                this.selectiveDisclosure = SDJwtService.getService().toSDMap(combinedSdJwt)
            }
        }

        fun fromString(data: String): VerifiableCredential {
            return when {
                isJWT(data) -> fromJwt(data)
                isSDJwt(data) -> fromSdJwt(data)
                else -> fromJson(data)
            }
        }
    }
}

fun String.toVerifiableCredential(): VerifiableCredential {
    val vc = VerifiableCredential.fromString(this)
    return if (vc.type.contains("VerifiablePresentation")) {
        VerifiablePresentation.fromVerifiableCredential(vc)
    } else {
        return vc
    }
}

fun <T> VerifiableCredential.verifyByFormatType(jwt: (String) -> T, ld: (String) -> T): T = when (this.jwt) {
    null -> ld(this.encode())
    else -> jwt(this.encode())
}
