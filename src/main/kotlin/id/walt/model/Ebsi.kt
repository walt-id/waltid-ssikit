@file:UseSerializers(DateAsIso8601UtcStringSerializer::class)

package id.walt.model

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import com.nimbusds.jose.jwk.ECKey
import id.walt.common.prettyPrint
import id.walt.crypto.decBase64
import id.walt.vclib.model.Proof
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import mu.KotlinLogging
import java.time.LocalDateTime


data class TrustedIssuerRegistry(
    val issuer: Issuer,
    // val accreditationCredentials: List<VerifiableCredential>
)

data class TrustedAccreditationOrganizationRegistry(
    val accreditationOrganization: AccreditationOrganization,
    val accreditationAuthorization: List<AccreditationAuthorization>,
    val proof: Proof
)

/*
data class Proof(
    //@Serializable(with = ProofTypeSerializer::class)
    var type: String,
    var created: LocalDateTime,
    var creator: String? = null,
    var proofPurpose: String? = null,
    //@Serializable(with = VerificationMethodCertSerializer::class)
    var verificationMethod: VerificationMethodCert? = null,
    var proofValue: String? = null,
    var domain: String? = null,
    var nonce: String? = null,
    var jws: String? = null
)*/

@Serializable
data class RevocationRegistry(
    val id: String,
    val version: String,
    val type: List<String>,
    val context: List<String>
)

@Serializable
data class SchemaRegistry(
    val id: String,
    val version: String,
    val type: List<String>,
    val context: List<String>
)


@Serializable
data class AccreditationOrganization(
    val preferredName: String,
    val domain: String,
    val did: List<String>,
    val eidasCertificate: EidasCertificate,
    val serviceEndpoints: List<ServiceEndpoint>,
    val accreditationPolicy: List<String>,
    val organizationInfo: OrganizationInfo
)

@Serializable
data class EssifAuthority(
    val preferredName: String,
    val did: List<String>,
    val eidasCertificatePem: String,
    val serviceEndpoints: List<String>,
    val organizationInfo: OrganizationInfo
)

@Serializable
data class AccreditationAuthorization(
    val credentialSchemaId: String,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val evidence: String
)

data class Issuer(
    val preferredName: String,
    val domain: String,
    val did: List<String>,
    val eidasCertificate: EidasCertificate,
    val serviceEndpoints: List<ServiceEndpoint>,
    val organizationInfo: OrganizationInfo,
    val proof: Proof
)

@Serializable
data class Attribute(
    val hash: String,
    val body: String,
)

private val attributeInfoLog = KotlinLogging.logger("AttributeInfo")

@Serializable
data class AttributeInfo(
    @SerialName("@context")
    @Json(name = "@context")
    val context: String = "",
    val type: String = "",
    val name: String = "",
    val data: String = ""
) {

    companion object {
        fun from(base64Body: String): AttributeInfo? {
            val decodedAttributeBody = String(decBase64(base64Body))
            try {
                return Klaxon().parse<AttributeInfo>(decodedAttributeBody)
            } catch(e: KlaxonException) {
                attributeInfoLog.debug("Klaxon error (${e.message}) for attribute body ($base64Body)")
                return AttributeInfo()
            }
        }
    }
}

@Serializable
data class TrustedIssuer(
    val did: String,
    val attributes: List<Attribute>,
)

fun TrustedIssuer.encode() = Klaxon().toJsonString(this)
fun TrustedIssuer.encodePretty() = encode().prettyPrint()

@Serializable
data class EidasCertificate(
    val eidasCertificateIssuerNumber: String,
    val eidasCertificateSerialNumber: String,
    val eidasCertificatePem: String
)

@Serializable
data class OrganizationInfo(
    val legalPersonalIdentifier: String,
    val legalName: String,
    val legalAddress: String,
    val VATRegistration: String,
    val taxReference: String,
    val LEI: String,
    val EORI: String,
    val SEED: String,
    val SIC: String,
    val domainName: String
)

@Serializable
data class CredentialStatusListEntry(
    val credentialID: String,
    val proofOfControl: String,
    val status: String,
    val lastModificationDate: LocalDateTime,
    val modificationReason: List<String>
)

@Serializable
data class AuthRequestResponse(val session_token: String)

@Serializable
data class OidcRequest(
    val uri: String,
    val callback: String
)

data class OidcAuthenticationRequestUri(
    val response_type: String,
    val scope: String,
    val request: String
)

//Old: data class DidAuthRequest(val reponse_type: String, val client_id: String, val scope: String, val nonce: String, val request: String)
@Serializable
data class DidAuthRequest(
    val reponse_type: String,
    val client_id: String,
    val scope: String,
    val nonce: String,
    val authenticationRequestJwt: AuthenticationRequestJwt?,
    val callback: String
)

@Serializable
data class AuthenticationRequestJwt(
    val authHeader: AuthenticationHeader,
    val authRequestPayload: AuthenticationRequestPayload
)

@Serializable
data class AuthenticationResponseJwt(
    val authHeader: AuthenticationHeader,
    val authRequestPayload: AuthenticationResponsePayload
)


@Serializable
data class DecryptedAccessTokenResponse(
    val access_token: String,
    val did: String,
    val nonce: String
)

@Serializable
data class AuthenticationHeader(
    val alg: String,
    val typ: String,
    val jwk: Jwk
)

@Serializable
data class AuthenticationRequestPayload(
    val scope: String,
    val iss: String,
    val response_type: String,
    val client_id: String,
    val nonce: String,
    val registration: AuthenticationRequestRegistration,
    val claims: Claim
)


@Serializable
data class AuthenticationRequestRegistration(
    val redirect_uris: List<String>,
    val response_types: String,
    val id_token_signed_response_alg: List<String>,
    val request_object_signing_alg: List<String>,
    val access_token_signing_alg: List<String>,
    val access_token_encryption_alg_values_supported: List<String>,
    val access_token_encryption_enc_values_supported: List<String>,
    val jwks_uri: String
)


@Serializable
data class Claim(
    val id_token: IdToken
)

@Serializable
data class SiopSessionRequest(
    val id_token: String
)

@Serializable
data class IdToken(
    val verified_claims: List<String>
)

@Serializable
data class VerifiedClaims(
    val verification: Verification
)

@Serializable
data class Verification(
    val trust_framework: String,
    val evidence: Evidence
)

@Serializable
data class Evidence(
    val type: Type,
    val document: Document
)

@Serializable
data class Document(
    @SerialName("type")
    val type: TypeDocument,
    @SerialName("credentialSchema")
    val credentialSchema: CredentialSchemaAr
)

@Serializable
data class CredentialSchemaAr(
    val id: Type
)


@Serializable
data class Type(
    val essential: Boolean? = null,
    val value: String
)

@Serializable
data class TypeDocument(
    val essential: Boolean? = null,
    val value: List<String>
)

@Serializable
data class AuthenticationResponsePayload(
    val aud: String,
    val sub: String,
    val iss: String,
    val claims: AuthenticationResponseVerifiedClaims,
    val sub_jwk: Jwk? = null,
    val exp: Long,
    val iat: Long,
    //val sub_did_verification_method_uri: String,
    val nonce: String
)

@Serializable
data class AuthenticationResponseVerifiedClaims(
    val verified_claims: String,
    val encryption_key: Jwk? = null
)

@Serializable
data class AccessTokenResponse(
    val ake1_enc_payload: String,
    val ake1_sig_payload: AkeSigPayload,
    val ake1_jws_detached: String,
    val did: String
)


@Serializable
data class AkeSigPayload(
    val iss: String,
    val did: String,
    val iat: Long,
    val exp: Long,
    val ake1_nonce: String,
    val ake1_enc_payload: String
)

@Serializable
data class Ake1EncPayload(
    val access_token: String,
    val did: String
)

@Serializable
data class Ake1JwsDetached(
    val ake1_nonce: String,
    val ake1_enc_payload: String,
    val did: String
)

@Serializable
data class EncryptedPayload(
    val iv: String,
    val ephemPublicKey: String,
    val mac: String,
    val cipherText: String
)


data class EncryptedAke1Payload(
    val iv: ByteArray,
    val ephemPublicKey: ECKey,
    val mac: ByteArray,
    val cipherText: ByteArray
)


@Serializable
data class AccessTokenHeader(
    val alg: String,
    val cty: String,
    val typ: String
)

@Serializable
data class AccessTokenPayload(
    val iss: String,
    val sub: String,
    val aud: String,
    val iat: Long,
    val exp: Long,
    val nonce: String,
    val atHash: String,
)
