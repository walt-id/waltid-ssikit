package org.letstrust.essif

import org.junit.Test
import org.letstrust.services.essif.UserWalletService

// https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/2.+Authorization+API
class AuthorizationApiTest {

    @Test
    fun testAuthApiFlow() {
        // https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=BLOCKCHAININT&title=2.+Authorization+API
        println("ESSIF Authorization API")

        // Verifiable Authorization must be previously installed via ESSIF onboarding flow (DID registration)
        val verifiableAuthorization = "{\n" +
                "  \"@context\": [\n" +
                "    \"https://www.w3.org/2018/credentials/v1\"\n" +
                "  ],\n" +
                "  \"id\": \"did:ebsi-eth:00000001/credentials/1872\",\n" +
                "  \"type\": [\n" +
                "    \"VerifiableCredential\",\n" +
                "    \"VerifiableAuthorization\"\n" +
                "  ],\n" +
                "  \"issuer\": \"did:ebsi:000001234\",\n" +
                "  \"issuanceDate\": \"2020-08-24T14:13:44Z\",\n" +
                "  \"expirationDate\": \"2020-08-25T14:13:44Z\",\n" +
                "  \"credentialSubject\": {\n" +
                "    \"id\": \"did:key:z6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5\",\n" +
                "    \"hash\": \"e96e3fecdbdf2126ea62e7c6...04de0f177e5971c27dedd0d17bc649a626ac\"\n" +
                "  },\n" +
                "  \"proof\": {\n" +
                "    \"type\": \"EcdsaSecp256k1Signature2019\",\n" +
                "    \"created\": \"2020-08-24T14:13:44Z\",\n" +
                "    \"proofPurpose\": \"assertionMethod\",\n" +
                "    \"verificationMethod\": \"did:ebsi-eth:000001234#key-1\",\n" +
                "    \"jws\": \"eyJhbGciOiJSUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..TCYt5X\"\n" +
                "  }\n" +
                "}\n"

        ///////////////////////////////////////////////////////////////////////////
        // Run authentication protocol (DID Auth + Authenticated Key Exchange Protocol)
        // and receive JWT Access Token + Authenticated Key Exchange Protocol.
        ///////////////////////////////////////////////////////////////////////////

        val accessToken =  UserWalletService.requestAccessToken(verifiableAuthorization)

        ///////////////////////////////////////////////////////////////////////////
        // Protected resource can now be accessed
        ///////////////////////////////////////////////////////////////////////////

        println("Accessing protected EBSI resource ...\n")
        UserWalletService.accessProtectedResource(accessToken)
        println("Accessed /protectedResource successfully ✔ ")

    }
}
