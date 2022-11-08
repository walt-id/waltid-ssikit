package id.walt.services.essif

import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.oauth2.sdk.util.URLUtils
import com.nimbusds.openid.connect.sdk.Nonce
import id.walt.crypto.KeyAlgorithm
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.model.oidc.CredentialAuthorizationDetails
import id.walt.model.oidc.IssuanceInitiationRequest
import id.walt.model.oidc.OIDCProvider
import id.walt.model.oidc.OIDCProviderWithMetadata
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.ecosystems.essif.EssifClient
import id.walt.services.ecosystems.essif.didebsi.DidEbsiService
import id.walt.services.ecosystems.essif.didebsi.EBSI_ENV_URL
import id.walt.services.key.KeyService
import id.walt.services.oidc.CompatibilityMode
import id.walt.services.oidc.OIDC4CIService
import id.walt.services.oidc.OIDC4VPService
import id.walt.services.oidc.OIDCUtils
import id.walt.signatory.*
import id.walt.signatory.dataproviders.DefaultDataProvider
import id.walt.signatory.dataproviders.MergingDataProvider
import id.walt.test.RESOURCES_PATH
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.CredentialSchema
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.model.toCredential
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.annotation.EnabledCondition
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.match
import io.ktor.http.*
import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

val EBSI_BEARER_TOKEN_FILE = "wct_bearer_token"

class WCTEnabled : EnabledCondition {
    override fun enabled(kclass: KClass<out Spec>): Boolean {
        return File(EBSI_BEARER_TOKEN_FILE).exists()
    }
}

/*
Start mitm dump using:
./mitmdump -s mitm-save.py -H "/Conformance/bbfdeea3-ee51-47db-b7c6-d286bbeafd51" -m reverse:https://api.conformance.intebsi.xyz -p 8080

 */

//@EnabledIf(WCTEnabled::class)
class WCTTest : AnnotationSpec() {
    val REAL_EBSI_WCT_URL = "https://api.conformance.intebsi.xyz"
    // WCT Conformance header: bbfdeea3-ee51-47db-b7c6-d286bbeafd51
    val EBSI_WCT_ENV = "http://localhost:8080"
    val SCHEMA_ID =
        "https://api.preprod.ebsi.eu/trusted-schemas-registry/v1/schemas/0x14b05b9213dbe7d343ec1fe1d3c8c739a3f3dc5a59bae55eb38fa0c295124f49#"
    val REDIRECT_URI = "http://blank"
    val STATE = "teststate"
    val NONCE = "testnonce"

    lateinit var did: String
    //lateinit var ebsiBearerToken: String

    @BeforeAll
    fun init() {
        EBSI_ENV_URL = EBSI_WCT_ENV
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
        val key = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
        did = DidService.create(DidMethod.ebsi, key.id, DidService.DidEbsiOptions(2))

        //ebsiBearerToken = File(EBSI_BEARER_TOKEN_FILE).readText().trim()
    }

    //@Test
//    fun testDidRegistration() {
//        EBSI_ENV_URL shouldBe EBSI_WCT_ENV
//        shouldNotThrowAny {
//            EssifClient.onboard(did, ebsiBearerToken)
//            EssifClient.authApi(did)
//            DidEbsiService.getService().registerDid(did, did)
//        }
//    }

    @Test
    fun testIssuanceFlow() {
        shouldNotThrowAny {
            DidService.resolve(did)
        }
        val oidcProvider = OIDCProvider("EBSI issuer", "$EBSI_WCT_ENV/conformance/v2")
        val issuer = OIDCProviderWithMetadata(
            id = oidcProvider.id,
            url = oidcProvider.url,
            oidc_provider_metadata = OIDC4CIService.getEbsiConformanceIssuerMetadata(oidcProvider, true)!!
        )
        val initiationRequestUri = "openid://initiate_issuance?issuer=${URLEncoder.encode(EBSI_WCT_ENV, StandardCharsets.UTF_8)}%2Fconformance%2Fv2&credential_type=https%3A%2F%2Fapi.conformance.intebsi.xyz%2Ftrusted-schemas-registry%2Fv2%2Fschemas%2FzCfNxx5dMBdf4yVcsWzj1anWRuXcxrXj1aogyfN1xSu8t&conformance=bbfdeea3-ee51-47db-b7c6-d286bbeafd51"
        val initiationRequest = IssuanceInitiationRequest.fromQueryParams(URLUtils.parseParameters(URI.create(initiationRequestUri).query))

        val authUri = OIDC4CIService.getUserAgentAuthorizationURL(issuer, URI.create(REDIRECT_URI), initiationRequest.credential_types.map {
            CredentialAuthorizationDetails(it, "jwt_vc")
        }, op_state = initiationRequest.op_state)
        authUri shouldNotBe null

        val response = AuthorizationRequest.parse(authUri).toHTTPRequest(HTTPRequest.Method.GET).apply {
            followRedirects = false
        }.send()
        response.statusCode shouldBe HttpStatusCode.Found.value

        val code = OIDCUtils.getCodeFromRedirectUri(response.location)
        code shouldNotBe null

        val tokens = OIDC4CIService.getAccessToken(issuer, code!!, REDIRECT_URI)

        val vc = OIDC4CIService.getCredential(issuer, tokens.oidcTokens.accessToken, initiationRequest.credential_types.first(),
            OIDC4CIService.generateDidProof(OIDCProvider(REAL_EBSI_WCT_URL, "$REAL_EBSI_WCT_URL/conformance/v2"), did, tokens.customParameters["c_nonce"].toString()), "jwt_vc")

        vc shouldNotBe null
    }

    @Test
    fun testVerificationFlow() {
        val vcJwt = Signatory.getService().issue("VerifiableAttestation", ProofConfig(
            issuerDid = did, subjectDid = did, proofType = ProofType.JWT, ecosystem = Ecosystem.ESSIF
        ), dataProvider = object : SignatoryDataProvider {
            override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential {
                val populatedVc = DefaultDataProvider.populate(template, proofConfig)
                return MergingDataProvider(mapOf("credentialSchema" to mapOf("id" to "https://api.conformance.intebsi.xyz/trusted-schemas-registry/v2/schemas/z3kRpVjUFj4Bq8qHRENUHiZrVF5VgMBUe7biEafp1wf2J"))).populate(populatedVc, proofConfig)
            }

        })
        val vc = vcJwt.toCredential()
        Custodian.getService().storeCredential(vc.id!!, vc)

        val oidcUrl = "openid://?scope=openid&response_type=id_token&client_id=https%3A%2F%2Fapi.conformance.intebsi.xyz%2Fconformance%2Fv2%2Fverifier-mock%2Fauthentication-responses&redirect_uri=https%3A%2F%2Fapi.conformance.intebsi.xyz%2Fconformance%2Fv2%2Fverifier-mock%2Fauthentication-responses&claims=%7B%22id_token%22%3A%7B%22email%22%3Anull%7D%2C%22vp_token%22%3A%7B%22presentation_definition%22%3A%7B%22id%22%3A%22conformance_mock_vp_request%22%2C%22input_descriptors%22%3A%5B%7B%22id%22%3A%22conformance_mock_vp%22%2C%22name%22%3A%22Conformance%20Mock%20VP%22%2C%22purpose%22%3A%22Only%20accept%20a%20VP%20containing%20a%20Conformance%20Mock%20VA%22%2C%22constraints%22%3A%7B%22fields%22%3A%5B%7B%22path%22%3A%5B%22%24.vc.credentialSchema%22%5D%2C%22filter%22%3A%7B%22allOf%22%3A%5B%7B%22type%22%3A%22array%22%2C%22contains%22%3A%7B%22type%22%3A%22object%22%2C%22properties%22%3A%7B%22id%22%3A%7B%22type%22%3A%22string%22%2C%22pattern%22%3A%22https%3A%2F%2Fapi.conformance.intebsi.xyz%2Ftrusted-schemas-registry%2Fv2%2Fschemas%2Fz3kRpVjUFj4Bq8qHRENUHiZrVF5VgMBUe7biEafp1wf2J%22%7D%7D%2C%22required%22%3A%5B%22id%22%5D%7D%7D%5D%7D%7D%5D%7D%7D%5D%2C%22format%22%3A%7B%22jwt_vp%22%3A%7B%22alg%22%3A%5B%22ES256K%22%5D%7D%7D%7D%7D%7D&nonce=3cbb22d1-69c9-4d0f-94ef-759c7870b19c&conformance=bbfdeea3-ee51-47db-b7c6-d286bbeafd51"

        val authReq = OIDC4VPService.parseOIDC4VPRequestUri(URI.create(oidcUrl))
        val presentationDefinition = OIDC4VPService.getPresentationDefinition(authReq)
        val matchingVcs = OIDCUtils.findCredentialsFor(presentationDefinition)

        matchingVcs.values.flatten() shouldContain vc.id

        val nonce = authReq.getCustomParameter("nonce").firstOrNull()
        nonce shouldBe "3cbb22d1-69c9-4d0f-94ef-759c7870b19c"

        val vp = Custodian.getService().createPresentation(listOf(vcJwt), did, challenge = nonce, expirationDate = null).toCredential() as VerifiablePresentation
        val resp = OIDC4VPService.getSIOPResponseFor(authReq, did, listOf(vp))
        val uri = OIDC4VPService.postSIOPResponse(authReq, resp)
        println(uri)
    }

}
