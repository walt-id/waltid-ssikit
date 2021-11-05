package id.walt.rest

import id.walt.model.AuthRequestResponse
import id.walt.rest.essif.EssifAPI
import id.walt.services.essif.mock.RelyingParty
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

class EssifApiTest : AnnotationSpec() {

    val ESSIF_API_URL = "http://localhost:7012"

    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    @BeforeClass
    fun startServer() {
        EssifAPI.start(7012)
    }

    @AfterClass
    fun teardown() {
        EssifAPI.stop()
    }

    @Test
    fun testHealth() = runBlocking {
        val response = client.get<HttpResponse>("$ESSIF_API_URL/health")
        "OK" shouldBe response.readText()
    }

    @Test
    fun testRealEbsi() = runBlocking {
        val authResp = client.post<AuthRequestResponse>("$ESSIF_API_URL/test/enterprise/wallet/authentication-requests") {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Accept, "application/json")
            }
            body = mapOf("scope" to "ebsi users onboarding")
        }
        println(authResp)
    }

    @Test
    fun testOnboarding() = runBlocking {
        println("ESSIF onboarding of a Legal Entity by requesting a Verifiable ID")

        val credentialRequestUri = client.get<String>("$ESSIF_API_URL/test/ti/requestCredentialUri")
        println(credentialRequestUri)

        val didOwnershipReq = client.post<String>("$ESSIF_API_URL/test/ti/requestVerifiableCredential") {
            contentType(ContentType.Application.Json)
        }
        println(didOwnershipReq)

        val didOfLegalEntity = client.post<String>("$ESSIF_API_URL/test/enterprise/wallet/createDid") {
            contentType(ContentType.Application.Json)
        }
        println(didOfLegalEntity)

        val verifiableId = client.post<String>("$ESSIF_API_URL/test/enterprise/wallet/getVerifiableCredential") {
            contentType(ContentType.Application.Json)
            // body = GetVcRequest("did:ebsi:234567", "did-ownership-req")
        }
        println(verifiableId)
    }

//   //TODO @Test
//    fun testAuthApi() = runBlocking {
//        println("ESSIF Authorization API")
//
//        // Verifiable Authorization must be previously installed via ESSIF onboarding flow (DID registration)
//        val verifiableAuthorization = "{\n" +
//                "  \"@context\": [\n" +
//                "    \"https://www.w3.org/2018/credentials/test\"\n" +
//                "  ],\n" +
//                "  \"id\": \"did:ebsi-eth:00000001/credentials/1872\",\n" +
//                "  \"type\": [\n" +
//                "    \"VerifiableCredential\",\n" +
//                "    \"VerifiableAuthorization\"\n" +
//                "  ],\n" +
//                "  \"issuer\": \"did:ebsi:000001234\",\n" +
//                "  \"issuanceDate\": \"2020-08-24T14:13:44Z\",\n" +
//                "  \"expirationDate\": \"2020-08-25T14:13:44Z\",\n" +
//                "  \"credentialSubject\": {\n" +
//                "    \"id\": \"did:key:z6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5\",\n" +
//                "    \"hash\": \"e96e3fecdbdf2126ea62e7c6...04de0f177e5971c27dedd0d17bc649a626ac\"\n" +
//                "  },\n" +
//                "  \"proof\": {\n" +
//                "    \"type\": \"EcdsaSecp256k1Signature2019\",\n" +
//                "    \"created\": \"2020-08-24T14:13:44Z\",\n" +
//                "    \"proofPurpose\": \"assertionMethod\",\n" +
//                "    \"verificationMethod\": \"did:ebsi-eth:000001234#key-1\",\n" +
//                "    \"jws\": \"eyJhbGciOiJSUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..TCYt5X\"\n" +
//                "  }\n" +
//                "}\n"
//
//        val accessToken = client.post<String>("$ESSIF_API_URL/test/user/wallet/requestAccessToken") {
//            contentType(ContentType.Application.Json)
//            body = verifiableAuthorization
//        }
//
//        println(accessToken)
//
//        //UserWalletService.accessProtectedResource(accessToken) // e.g updateDID, revoke VC
//    }

    @Test
    fun testVcIssuance() = runBlocking {
        println("Credential issuance from a Legal Entity (EOS/Trusted Issuer) to a Natural Person.")

        val didAuthRequest = client.post<String>("$ESSIF_API_URL/test/ti/credentials")

        println(didAuthRequest)

        val resp = client.post<HttpResponse>("$ESSIF_API_URL/test/user/wallet/validateDidAuthRequest") {
            contentType(ContentType.Application.Json)
            body = didAuthRequest
        }
        200 shouldBe resp.status.value

        val vcToken = client.post<String>("$ESSIF_API_URL/test/user/wallet/didAuthResponse") {
            contentType(ContentType.Application.Json)
            body = didAuthRequest
        }
        println(vcToken)

        val credential = client.post<String>("$ESSIF_API_URL/test/ti/credentials") {
            contentType(ContentType.Application.Json)
            body = didAuthRequest
        }
        println(credential)
    }

    @Test
    fun testVcExchange() = runBlocking {
        println("ESSIF Verifiable Credential Exchange from an Natural Person (Holder) to a Legal Entity")

        val vcExchangeRequest = RelyingParty.signOn()

        val vcToken = client.post<String>("$ESSIF_API_URL/test/user/wallet/vcAuthResponse") {
            contentType(ContentType.Application.Json)
            body = vcExchangeRequest
        }
        println(vcToken)

//        UserWalletService.vcAuthResponse(vcExchangeRequest)
//        println("15. Credentials share successfully")
//
//        RelyingParty.getSession("sessionId")
//        println("18. Process completed successfully")
    }
}
