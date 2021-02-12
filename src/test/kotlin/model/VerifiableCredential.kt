package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class VerifiableCredential(

    @SerialName("@context") val context: List<String>,
    val id: String,
    val type: List<String>,
    val issuer: String,
    @Serializable(with = DateAsIso8601UtcStringSerializer::class) val issuanceDate: LocalDateTime,
    val credentialSubject: CredentialSubject,
    val credentialStatus: CredentialStatus? = null,
    val credentialSchema: CredentialSchema? = null,
    val proof: Proof
)

@Serializable
data class ServiceEndpoint(
    val id: String,
    val type: String,
    var serviceEndpoint: String,
)


@Serializable
data class CredentialSchema(
    val id: String,
    val type: String
)

@Serializable
data class CredentialSubject(
    val did: String? = null,
    val id: String? = null,
    val authorizationClaims: List<String>? = null,
    val naturalPerson: NaturalPerson? = null
)

@Serializable
data class NaturalPerson(
    val did: String? = null,
    val publicKey: String? = null,
    val serviceEndpoints: List<ServiceEndpoint>
)

@Serializable
data class CredentialStatus(
    val id: String,
    val type: String
)

@Serializable
data class Proof(
    val type: String,
    @Serializable(with = DateAsIso8601UtcStringSerializer::class) val created: LocalDateTime,
    val proofPurpose: String,
    @Serializable(with = VerificationMethodSerializer::class) val verificationMethod: VerificationMethod,
    val proofValue: String? = null,
    val jws: String? = null
)

@Serializable
data class VerificationMethod(
    val type: String? = null,
    val CertSerial: String? = null
)
