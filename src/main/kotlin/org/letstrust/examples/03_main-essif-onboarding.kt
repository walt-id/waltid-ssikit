package org.letstrust.examples

import mu.KotlinLogging
import org.letstrust.s.essif.EosService
import org.letstrust.services.essif.EnterpriseWalletService

private val log = KotlinLogging.logger {}

fun main() {

    ///////////////////////////////////////////////////////////////////////////
    // Prerequisite: The LE must be authenticated and authorized by the classical
    // way before triggering the ESSIF onboarding flow.
    ///////////////////////////////////////////////////////////////////////////

    println("ESSIF onboarding of a Legal Entity by requesting a Verifiable ID")

    ///////////////////////////////////////////////////////////////////////////
    // LE requests the credential URI from the ESSIF Onboarding Service (EOS)
    ///////////////////////////////////////////////////////////////////////////
    println("1 Request V.ID (manually)")
    val credentialRequestUri = EosService.requestCredentialUri()
    log.debug { "credentialRequest: $credentialRequestUri" }

    ///////////////////////////////////////////////////////////////////////////
    // The EnterpriseWalletService processes the credentialRequestUri and forwards
    // the credential request ([POST] /credentials with and empty body) to the EOS
    ///////////////////////////////////////////////////////////////////////////
    println("3 Trigger Wallet")
    val didOwnershipReq = EnterpriseWalletService.requestVerifiableId(credentialRequestUri)

    ///////////////////////////////////////////////////////////////////////////
    // The DID ownership request is a JWT, which must be verified bey the public key of the EOS.
    //
    // Header:
    // {
    //  "alg": "ES256K",
    //  "typ": "JWT",
    //  "jwk": "{DID of the EOS}"
    //}
    //
    // The body looks as follows:
    // {
    //  "id": "<random-id>",
    //  "type": "credential-request",
    //  "iss": "did:ebsi:2123456789AB...fghijkmno",
    //  "callback": "<callback-url>",
    //  "nonce": "<random-nonce>",
    //  "claims": {
    //    "types": [
    //      {
    //        "id": "https://essif.europa.eu/tsr-vid/verifiableid1.json",
    //        "type": "JsonSchemaValidator2018"
    //      }
    //    ]
    //  }
    //}
    ///////////////////////////////////////////////////////////////////////////
    log.debug { "didOwnershipReq: $didOwnershipReq" }
    println("6. Notify DID ownership request")
    println("7. Create a new DID")

    ///////////////////////////////////////////////////////////////////////////
    // Creation of DID sub-flow (see: 04_main-essif-register-did.kt)
    ///////////////////////////////////////////////////////////////////////////
    val didOfLegalEntity = EnterpriseWalletService.createDid()

    ///////////////////////////////////////////////////////////////////////////
    // Once the DID is created, the DID ownership response composed and set to the EOS
    //
    // The DID of the Legal Entity is placed in the header:
    // {
    //  "alg": "ES256K",
    //  "typ": "JWT",
    //  "jwk": "{DID of LE}"
    //}
    //
    // The body returns the request-nonce among
    // {
    //  "nonce": "<request-nonce>",
    //  "type": "credential-response",
    //  "claims": {
    //    "types": [
    //      {
    //        "id": "https://essif.europa.eu/tsr-vid/verifiableid1.json",
    //        "type": "JsonSchemaValidator2018"
    //      }
    //    ]
    //  }
    //}
    ///////////////////////////////////////////////////////////////////////////


    val verifiableId = EnterpriseWalletService.getVerifiableId(didOwnershipReq, didOfLegalEntity)

    ///////////////////////////////////////////////////////////////////////////
    // The reqeuested V.ID is returned and should look like the following::
    // {
    //   "@context": [
    //     "https://www.w3.org/2018/credentials/v1",
    //     "https://essif.europa.eu/schemas/v-id/2020/v1",
    //     "https://essif.europa.eu/schemas/eidas/2020/v1"
    //   ],
    //   "id": "identity#verifiableID#1dee355d-0432-4910-ac9c-70d89e8d674e",
    //   "type": [
    //     "VerifiableCredential",
    //     "VerifiableID"
    //   ],
    //   "issuer": "did:ebsi:2123456789AB...fghijkmno",
    //   "issuanceDate": "2019-06-22T14:11:44Z",
    //   "validFrom": "2019-06-22T14:11:44Z",
    //   "credentialSubject": {
    //     "id": "did:ebsi:2123456789AB...fghijkmno",
    //     "legalPersonalIdentifier": "123456789",
    //     "legalName": "Example Company",
    //   },
    //   "credentialStatus": {
    //     "id": "https://essif.europa.eu/status/identity#verifiableID#1dee355d-0432-4910-ac9c-70d89e8d674e",
    //     "type": "CredentialStatusList2020"
    //   },
    //   "credentialSchema": {
    //     "id": "https://essif.europa.eu/tsr-vid/verifiableid1.json",
    //     "type": "JsonSchemaValidator2018"
    //   },
    //   "evidence": [{
    //     "id": "https://essif.europa.eu/tsr-vid/evidence/f2aeec97-fc0d-42bf-8ca7-0548192d4231",
    //     "type": ["DocumentVerification"],
    //     "verifier": "did:ebsi:2123456789AB...fghijkmno",
    //     "evidenceDocument": "Passport",
    //     "subjectPresence": "Physical",
    //     "documentPresence": "Physical"
    //   }],
    //   "proof": {
    //     "type": "EidasSeal2021",
    //     "created": "2019-06-22T14:11:44Z",
    //     "proofPurpose": "assertionMethod",
    //     "verificationMethod": "did:ebsi:2123456789AB...fghijkmno#1088321447",
    //     "jws": "BD21J4fdlnBvBA+y6D...fnC8Y="
    //   }
    //}
    ///////////////////////////////////////////////////////////////////////////

    log.debug { "verifiableId: $verifiableId" }
    println("14. Successful process")

}
