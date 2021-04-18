package org.letstrust.examples

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.letstrust.model.DidMethod
import org.letstrust.model.VerifiableCredential


import org.letstrust.services.did.DidService
import org.letstrust.services.vc.CredentialService

val credentialTemplate1 = "{\n" +
        "    \"@context\": [\n" +
        "        \"https://www.w3.org/2018/credentials/v1\"\n" +
        "    ],\n" +
        "    \"id\": \"XXX\",\n" +
        "    \"type\": [\n" +
        "        \"VerifiableCredential\",\n" +
        "        \"VerifiableAttestation\"\n" +
        "    ],\n" +
        "    \"issuer\": \"XXX\",\n" +
        "    \"issuanceDate\": \"2021-03-11T12:53:12Z\",\n" +
        "    \"expirationDate\": \"2022-03-11T12:53:12Z\",\n" +
        "    \"credentialSubject\": {\n" +
        "        \"id\": \"XXX\",\n" +
        "        \"authorizationClaims\": [\n" +
        "            \"claim1\",\n" +
        "            \"claim2\"\n" +
        "        ]\n" +
        "    },\n" +
        "    \"credentialStatus\": {\n" +
        "        \"id\": \"https://essif.europa.eu/status\",\n" +
        "        \"type\": \"CredentialsStatusList2020\"\n" +
        "    },\n" +
        "    \"credentialSchema\": {\n" +
        "        \"id\": \"https://essif.europa.eu/tsr/education/CSR1224.json\",\n" +
        "        \"type\": \"JsonSchemaValidator2018\"\n" +
        "    }\n" +
        "}"

fun main() {

    println("LetsTrust SSI Basic Example")

    ///////////////////////////////////////////////////////////////////////////
    // Create Decentralized Identifiers including a keypair for issuer & holder
    ///////////////////////////////////////////////////////////////////////////


    val didIssuer = DidService.create(DidMethod.key)
    val didHolder = DidService.create(DidMethod.key)

    println("DID created (issuer): $didIssuer")
    println("DID created (holder): $didHolder")

    val didDocIssuer = DidService.resolve(didIssuer)
    println("\nDID Document resolved (issuer):\n$didDocIssuer")
    val didDocHolder = DidService.resolve(didHolder)
    println("\nDID Document resolved (holder):\n$didDocHolder")


    ///////////////////////////////////////////////////////////////////////////
    // Issue Verifiable Credential (by Issuer)
    ///////////////////////////////////////////////////////////////////////////

    val credentialTemplate = Json.decodeFromString<VerifiableCredential>(credentialTemplate1)

    credentialTemplate.issuer = didIssuer
    credentialTemplate.credentialSubject.id = didHolder

    val credOffer = Json.encodeToString(credentialTemplate)

    val signedCredential = CredentialService.sign(didIssuer, credOffer)

    println("\nVerifiable Credential issued:\n$signedCredential")


    ///////////////////////////////////////////////////////////////////////////
    // Verify Verifiable Credential (by Holder)
    ///////////////////////////////////////////////////////////////////////////

    val verifyCredential = CredentialService.verifyVc(signedCredential)

    println("\nVerifiable Credential verified: $verifyCredential")

    ///////////////////////////////////////////////////////////////////////////
    // Create Verifiable Presentation (by Holder)
    ///////////////////////////////////////////////////////////////////////////

    val presentation = CredentialService.present(signedCredential, "example.com", "asdf")

    println("\nVerifiable Presentation issued:\n$presentation")


    ///////////////////////////////////////////////////////////////////////////
    // Verify Verifiable Presentation (by Verifier)
    ///////////////////////////////////////////////////////////////////////////

    val verifyPresentation = CredentialService.verify(presentation)

    println("\nVerifiable Presentation verified: $verifyPresentation")
}
