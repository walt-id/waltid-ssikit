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
import id.walt.test.RESOURCES_PATH
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.model.toCredential
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.annotation.EnabledCondition
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

@EnabledIf(WCTEnabled::class)
class WCTTest : AnnotationSpec() {
    // WCT Conformance header: bbfdeea3-ee51-47db-b7c6-d286bbeafd51
    val EBSI_WCT_ENV = "http://localhost:8080"
    val REAL_EBSI_WCT_URL = "https://api.conformance.intebsi.xyz"
    val SCHEMA_ID =
        "https://api.preprod.ebsi.eu/trusted-schemas-registry/v1/schemas/0x14b05b9213dbe7d343ec1fe1d3c8c739a3f3dc5a59bae55eb38fa0c295124f49#"
    val REDIRECT_URI = "http://blank"
    val STATE = "teststate"
    val NONCE = "testnonce"

    lateinit var did: String
    //lateinit var ebsiBearerToken: String
    var vc: VerifiableCredential? = null

    @BeforeAll
    fun init() {
        EBSI_ENV_URL = EBSI_WCT_ENV
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
        val key = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256r1)
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

        vc = OIDC4CIService.getCredential(issuer, tokens.oidcTokens.accessToken, initiationRequest.credential_types.first(),
            OIDC4CIService.generateDidProof(OIDCProvider(REAL_EBSI_WCT_URL, "$REAL_EBSI_WCT_URL/conformance/v2"), did, tokens.customParameters["c_nonce"].toString()), "jwt_vc")

        vc shouldNotBe null
    }

    @Test
    fun testVerificationFlow() {
        //vc shouldNotBe null

//        val verifier = OIDCProvider("ebsi wct issuer", "$EBSI_WCT_ENV/conformance/v1/verifier-mock")
//
//        val siopReq = OIDC4VPService.fetchOIDC4VPRequest(verifier)
//        siopReq shouldNotBe null
//
//        val redirectUri = siopReq!!.redirectionURI
//        val ebsiEnvOverride = URI.create(EBSI_WCT_ENV)
//        val claims = OIDCUtils.getVCClaims(siopReq) // legacy spec
//
//        val siopReqMod = OIDC4VPService.createOIDC4VPRequest(
//            siopReq.requestURI,
//            redirect_uri = URI(
//                ebsiEnvOverride.scheme,
//                ebsiEnvOverride.authority,
//                redirectUri.path,
//                redirectUri.query,
//                redirectUri.fragment
//            ),
//            nonce = siopReq.getCustomParameter("nonce")?.firstOrNull()?.let { Nonce(it) } ?: Nonce(),
//            response_type = siopReq.responseType,
//            response_mode = siopReq.responseMode,
//            presentation_definition = claims.vp_token?.presentation_definition,
//            state = siopReq.state
//        )
//
//        val presentation = Custodian.getService().createPresentation(listOf(vc!!.encode()), did, expirationDate = null)
//            .toCredential() as VerifiablePresentation
//
//        val siopResponse = OIDC4VPService.getSIOPResponseFor(siopReqMod, did, listOf(presentation))
//        val result = OIDC4VPService.postSIOPResponse(siopReqMod, siopResponse, CompatibilityMode.EBSI_WCT)
//        (JSONParser(-1).parse(result) as JSONObject)["result"] shouldBe true
    }

}
