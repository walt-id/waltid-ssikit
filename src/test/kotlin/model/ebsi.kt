package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime


@Serializable
data class Tir(
    val issuer: Issuer,
    val accreditationCredentials: List<VerifiableCredential>
)

@Serializable
data class Taor(
    val accreditationOrganization: EssifAuthority,
    val accreditationAuthorization: List<AccreditationAuthorization>,
    @Serializable(with = DateAsIso8601UtcStringSerializer::class) val validFrom: LocalDateTime,
    @Serializable(with = DateAsIso8601UtcStringSerializer::class) val validUntil: LocalDateTime,
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
    @Serializable(with = DateAsIso8601UtcStringSerializer::class) val validFrom: LocalDateTime,
    @Serializable(with = DateAsIso8601UtcStringSerializer::class) val validUntil: LocalDateTime,
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
data class ServiceEndpoint(
    val id: String,
    val type: String,
    var serviceEndpoint: String,
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
data class VerifiableCredential(

    @SerialName("@context") val context: List<String>,
    val id: String,
    val type: List<String>,
    val issuer: String,
    @Serializable(with = DateAsIso8601UtcStringSerializer::class) val issuanceDate: LocalDateTime,
    val credentialSubject: CredentialSubject,
    val credentialStatus: CredentialStatus,
    val credentialSchema: CredentialSchema,
    val proof: Proof
)

@Serializable
data class CredentialSubject(
    val did: String,
    val authorizationClaims: List<String>
)

@Serializable
data class CredentialStatus(
    val id: String,
    val type: String
)

@Serializable
data class CredentialStatusListEntry(
    val credentialID: String,
    val proofOfControl: String,
    val status: String,
    @Serializable(with = DateAsIso8601UtcStringSerializer::class) val lastModificationDate: LocalDateTime,
    val modificationReason: List<String>
)

@Serializable
data class CredentialSchema(
    val id: String,
    val type: String
)

@Serializable
data class Proof(
    val type: String,
    @Serializable(with = DateAsIso8601UtcStringSerializer::class) val created: LocalDateTime,
    val proofPurpose: String,
    val verificationMethod: VerificationMethod,
    val proofValue: String
)

@Serializable
data class VerificationMethod(
    val type: String,
    val CertSerial: String
)
