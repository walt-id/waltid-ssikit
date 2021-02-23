@file:UseSerializers(DateAsIso8601UtcStringSerializer::class)

package org.letstrust.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonObject
import java.time.LocalDateTime

@Serializable
data class VerifiableCredential(

    @SerialName("@context")
    var context: List<String>,
    var id: String? = null,
    var type: List<String>,
    var issuer: String,
    var issuanceDate: LocalDateTime,
    var credentialSubject: CredentialSubject,
    var credentialStatus: CredentialStatus? = null,
    var credentialSchema: CredentialSchema? = null,
    var proof: Proof? = null,
    var another: JsonObject? = null
)

@Serializable
data class ServiceEndpoint(
    var id: String,
    var type: String,
    var serviceEndpoint: String,
)

@Serializable
data class CredentialSchema(
    var id: String,
    var type: String
)

@Serializable
data class VerifiableCredentialPRC(

    @SerialName("@context")
    var context: List<String>,
    var type: List<String>,
    var credentialSubject: CredentialSubjectPRC,
    var issuer: String,
    var issuanceDate: LocalDateTime,
    var identifier: String? = null,
    var name: String? = null,
    var description: String? = null,
    var proof: Proof,
    var another: JsonObject? = null
)

@Serializable
data class CredentialSubjectPRC(
    var did: String? = null,
    var id: String? = null,
    var type: List<String>? = null,
    var authorizationClaims: List<String>? = null,
    var naturalPerson: NaturalPerson? = null,
    var givenName: String? = null,
    var familyName: String? = null,
    var gender: String? = null,
    var image: String? = null,
    var residentSince: String? = null,
    var lprCategory: String? = null,
    var lprNumber: String? = null,
    var commuterClassification: String? = null,
    var birthCountry: String? = null,
    var birthDate: String? = null,
)

@Serializable
data class CredentialSubject(
    var did: String? = null,
    var id: String? = null,
    var authorizationClaims: List<String>? = null,
    var naturalPerson: NaturalPerson? = null
)

@Serializable
data class NaturalPerson(
    var did: String? = null,
    var publicKey: String? = null,
    var serviceEndpoints: List<ServiceEndpoint>
)

@Serializable
data class CredentialStatus(
    var id: String,
    var type: String
)

@Serializable
data class Proof(
    var type: String,
    var created: LocalDateTime,
    var proofPurpose: String,
    @Serializable(with = VerificationMethodCertSerializer::class)
    var verificationMethod: VerificationMethodCert,
    var proofValue: String? = null,
    var jws: String? = null
)

@Serializable
data class VerificationMethodCert(
    var type: String? = null,
    var CertSerial: String? = null
)
