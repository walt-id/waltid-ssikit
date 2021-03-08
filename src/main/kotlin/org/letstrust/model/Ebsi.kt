@file:UseSerializers(DateAsIso8601UtcStringSerializer::class)

package org.letstrust.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime


@Serializable
data class TrustedIssuerRegistry(
    val issuer: Issuer,
    val accreditationCredentials: List<VerifiableCredential>
)

@Serializable
data class TrustedAccreditationOrganizationRegistry(
    val accreditationOrganization: EssifAuthority,
    val accreditationAuthorization: List<AccreditationAuthorization>,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val evidence: String
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

@Serializable
data class Issuer(
    val preferredName: String,
    val did: List<String>,
    val eidasCertificate: EidasCertificate,
    val serviceEndpoints: List<ServiceEndpoint>,
    val organizationInfo: OrganizationInfo
)

@Serializable
data class EidasCertificate(
    val eidasCertificateIssuerNumber: String,
    val eidasCertificateSerialNumber: String,
    val eidasCertificatePem: String
)

@Serializable
data class OrganizationInfo(
    val id: String,
    val legalName: String,
    val currentAddress: String,
    val vatNumber: String,
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

data class OidcAuthenticationRequestUri(
    val response_type: String,
    val scope: String,
    val request: String
)

@Serializable
data class AuthenticationRequestHeader(
    val alg: String,
    val typ: String,
    val jwk: String
)

@Serializable
data class AuthenticationRequestPayload(
    val scope: String,
    val iss: String,
    val response_type: String,
    val client_id: String,
    val nonce: String,
    val registration: String,
    val claims: Claim
)

@Serializable
data class Claim(
    val id_token: IdToken
)

@Serializable
data class IdToken(
    val verified_claims: VerifiedClaims
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
    val iss: String,
    val sub: String,
    val aud: String,
    val iat: Long,
    val exp: Long,
    val sub_jwk: String,
    val sub_did_verification_method_uri: String,
    val nonce: String,
    val claims: AuthenticationResponseVerifiedClaims,
)

@Serializable
data class AuthenticationResponseVerifiedClaims(
    val verified_claims: String,
    val encryption_key: String

)
