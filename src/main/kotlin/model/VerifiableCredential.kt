@file:UseSerializers(DateAsIso8601UtcStringSerializer::class)

package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonObject
import java.time.LocalDateTime

@Serializable
data class VerifiableCredential(

    @SerialName("@context")
    val context: List<String>,
    val id: String,
    val type: List<String>,
    val issuer: String,
    val issuanceDate: LocalDateTime,
    val credentialSubject: CredentialSubject,
    val credentialStatus: CredentialStatus? = null,
    val credentialSchema: CredentialSchema? = null,
    val proof: Proof,
    val another: JsonObject? = null
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
data class VerifiableCredentialPRC(

    @SerialName("@context")
    val context: List<String>,
    val type: List<String>,
    val credentialSubject: CredentialSubjectPRC,
    val issuer: String,
    val issuanceDate: LocalDateTime,
    val identifier: String? = null,
    val name: String? = null,
    val description: String? = null,
    val proof: Proof,
    val another: JsonObject? = null
)

@Serializable
data class CredentialSubjectPRC(
    val did: String? = null,
    val id: String? = null,
    val type: List<String>? = null,
    val authorizationClaims: List<String>? = null,
    val naturalPerson: NaturalPerson? = null,
    val givenName: String? = null,
    val familyName: String? = null,
    val gender: String? = null,
    val image: String? = null,
    val residentSince: String? = null,
    val lprCategory: String? = null,
    val lprNumber: String? = null,
    val commuterClassification: String? = null,
    val birthCountry: String? = null,
    val birthDate: String? = null,
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
    val created: LocalDateTime,
    val proofPurpose: String,
    @Serializable(with = VerificationMethodSerializer::class)
    val verificationMethod: VerificationMethod,
    val proofValue: String? = null,
    val jws: String? = null
)

@Serializable
data class VerificationMethod(
    val type: String? = null,
    val CertSerial: String? = null
)
