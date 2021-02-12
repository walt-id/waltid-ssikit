@file:UseSerializers(DateAsIso8601UtcStringSerializer::class)

package model


import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
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

