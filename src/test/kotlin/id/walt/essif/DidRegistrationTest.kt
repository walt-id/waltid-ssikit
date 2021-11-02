package id.walt.essif

import id.walt.services.essif.enterprisewallet.EnterpriseWalletService
import id.walt.services.essif.mock.DidRegistry
import io.kotest.core.spec.style.AnnotationSpec
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

// Subflow from EBSI Onboarding Flow
// https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/4.+SUB-Flow%3A+DID%28-key%29+Registration+-++DID+Registration+flow
class DidRegistrationTest : AnnotationSpec() {

    private val enterpriseWalletService = EnterpriseWalletService.getService()

    @Test
    fun testEbsiDidRegistration() {

        ///////////////////////////////////////////////////////////////////////////
        // Generate ETH address + DID
        ///////////////////////////////////////////////////////////////////////////

        val did = enterpriseWalletService.didGeneration()

        ///////////////////////////////////////////////////////////////////////////
        // Obtain Verifiable Authorization by following steps
        // - Precondition: Access Token needs to be obtained via EU login (handled by parent flow)
        // - Proof control over DID:
        //
        // Request-JWT-Header:
        //{
        //  "alg": "ES256K",
        //  "typ": "JWT",
        //  "jwk": "{EOS-DID Key}",
        //}
        // Request-JWT-Body:
        // {
        //  "id": "<random-id>",
        //  "iss": "did:ebsi:2123456789AB...fghijkmno",
        //  "callback": "<callback-url>"
        //  "nonce": "<random-nonce>",
        //}
        //
        // Response-JWT-Header:
        // {
        //  "alg": "ES256K",
        //  "typ": "JWT",
        //  "jwk": "{User Wallet-DID Key}",
        //}
        // Response-JWT-Body:
        // {
        //  "nonce": "<request-nonce>"
        //  "data": {
        //    "@context": [
        //      "https://w3.org/ns/did/v1,
        //      https: //ebsi.org/ns/did/v1"
        //    ],
        //    "id": "did:ebsi:2123456789AB...fghijkmno",
        //    "verificationMethod": [{
        //        "id": "did:ebsi:2123456789AB...fghijkmno#key-1",
        //        "type": "Ed25519VerificationKey2018",
        //        "controller": "did:ebsi:2123456789AB...fghijkmno",
        //        "publicKeyBase58": "5F5Q1qJbbvcyhnStAmkjF5rUBKe1XnHJHt2Tj49tTDpW"
        //      },
        //      {
        //        "id": "did:ebsi:2123456789AB...fghijkmno#key-2",
        //        "type": "X25519KeyAgreementKey2019",
        //        "controller": "did:ebsi:2123456789AB...fghijkmno",
        //        "publicKeyBase58": "GnxxE5rAFYoRS6af7XArmzh7gNS68XTAKjhNDRsfhrFJ"
        //      },
        //      {
        //        "id": "did:ebsi:2123456789AB...fghijkmno#key-3",
        //        "type": "EidasVerificationKey2021",
        //        "controller": "did:ebsi:2123456789AB...fghijkmno",
        //        "publicKeyPem": "-----BEGIN.."
        //      }
        //    ],
        //    "authentication": [
        //      "did:ebsi:2123456789AB...fghijkmno#key-3"
        //    ],
        //    "assertionMethod": [
        //      "did:ebsi:2123456789AB...fghijkmno#key-1"
        //    ],
        //    "keyAgreement": [
        //      "did:ebsi:2123456789AB...fghijkmno#key-2"
        //    ],
        //    "proof": {
        //      "type": "Ed25519Signature2018",
        //      "created": "2019-06-22T14:11:44Z",
        //      "proofPurpose": "assertionMethod",
        //      "verificationMethod": "did:ebsi:2123456789AB...fghijkmno#key-1",
        //      "jws": "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        //    }
        //  }
        //}
        // - Receive/store Verifiable Authorisation
        ///////////////////////////////////////////////////////////////////////////

        val verifiableAuthoriztation = enterpriseWalletService.requestVerifiableAuthorization(did)
        log.debug { "verifiableAuthoriztation: $verifiableAuthoriztation" }

        val unsignedTransaction = DidRegistry.insertDidDocument()
        println("16. [EWallet] 200 <unsigned transaction>")
        println("17. [EWallet] Generate <signed transaction>")
        val signedTransaction = ""
        DidRegistry.signedTransaction(signedTransaction)

    }
}
