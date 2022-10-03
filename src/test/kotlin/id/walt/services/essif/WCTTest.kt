package id.walt.services.essif

import com.nimbusds.openid.connect.sdk.Nonce
import id.walt.crypto.KeyAlgorithm
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.model.oidc.CredentialClaim
import id.walt.model.oidc.OIDCProvider
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.ecosystems.essif.didebsi.DidEbsiService
import id.walt.services.ecosystems.essif.didebsi.EBSI_ENV_URL
import id.walt.services.ecosystems.essif.EssifClient
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
import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import java.io.File
import java.net.URI
import kotlin.reflect.KClass

val EBSI_BEARER_TOKEN_FILE = "wct_bearer_token"

class WCTEnabled: EnabledCondition {
  override fun enabled(kclass: KClass<out Spec>): Boolean {
    return File(EBSI_BEARER_TOKEN_FILE).exists()
  }
}

@EnabledIf(WCTEnabled::class)
class WCTTest: AnnotationSpec() {
  // WCT Conformance header: 286dc8c9-15ce-4f4b-a32b-8ce5a5b7c4f5
  val EBSI_WCT_ENV = "http://localhost:8080"
  val SCHEMA_ID = "https://api.preprod.ebsi.eu/trusted-schemas-registry/v1/schemas/0x14b05b9213dbe7d343ec1fe1d3c8c739a3f3dc5a59bae55eb38fa0c295124f49#"
  val REDIRECT_URI = "http://blank"
  val STATE = "teststate"
  val NONCE = "testnonce"

  lateinit var did: String
  lateinit var ebsiBearerToken: String
  var vc: VerifiableCredential? = null

  @BeforeAll
  fun init() {
    EBSI_ENV_URL = EBSI_WCT_ENV
    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    val key = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
    did = DidService.create(DidMethod.ebsi, key.id)

    ebsiBearerToken = File(EBSI_BEARER_TOKEN_FILE).readText().trim()
  }

  @Test
  fun testDidRegistration() {
    EBSI_ENV_URL shouldBe EBSI_WCT_ENV
    shouldNotThrowAny {
      EssifClient.onboard(did, ebsiBearerToken)
      EssifClient.authApi(did)
      DidEbsiService.getService().registerDid(did, did)
    }
  }

  @Test
  fun testIssuanceFlow() {
    shouldNotThrowAny {
      DidService.resolve(did)
    }

    val issuer = OIDC4CIService.getWithProviderMetadata(OIDCProvider("ebsi wct issuer", "$EBSI_WCT_ENV/conformance/v1/issuer-mock"))

    val redirectUri = OIDC4CIService.executeGetAuthorizationRequest(issuer, URI.create(REDIRECT_URI), listOf(CredentialClaim(type = SCHEMA_ID, manifest_id = null)), nonce = NONCE, state = STATE)
    redirectUri shouldNotBe null

    val code = OIDCUtils.getCodeFromRedirectUri(redirectUri!!)
    code shouldNotBe null

    val tokenResponse = OIDC4CIService.getAccessToken(issuer, code!!, REDIRECT_URI, CompatibilityMode.EBSI_WCT)
    tokenResponse.indicatesSuccess() shouldBe true

    val proof = OIDC4CIService.generateDidProof(did, tokenResponse.customParameters["c_nonce"]?.toString())
    vc = OIDC4CIService.getCredential(issuer, tokenResponse.toSuccessResponse().oidcTokens.accessToken, did, SCHEMA_ID, proof, mode = CompatibilityMode.EBSI_WCT)
    vc shouldNotBe null
    vc?.credentialSchema shouldNotBe null
    vc?.credentialSchema?.id shouldBe SCHEMA_ID
  }

  @Test
  fun testVerificationFlow() {
    vc shouldNotBe null

    val verifier = OIDCProvider("ebsi wct issuer", "$EBSI_WCT_ENV/conformance/v1/verifier-mock")

    val siopReq = OIDC4VPService.fetchOIDC4VPRequest(verifier)
    siopReq shouldNotBe null

    val redirectUri = siopReq!!.redirectionURI
    val ebsiEnvOverride = URI.create(EBSI_WCT_ENV)
    val claims = OIDCUtils.getVCClaims(siopReq) // legacy spec

    val siopReqMod = OIDC4VPService.createOIDC4VPRequest(
      siopReq.requestURI,
      redirect_uri = URI(ebsiEnvOverride.scheme, ebsiEnvOverride.authority, redirectUri.path, redirectUri.query, redirectUri.fragment),
      nonce = siopReq.getCustomParameter("nonce")?.firstOrNull()?.let { Nonce(it) } ?: Nonce(),
      response_type = siopReq.responseType,
      response_mode = siopReq.responseMode,
      presentation_definition = claims.vp_token?.presentation_definition,
      state = siopReq.state
    )

    val presentation = Custodian.getService().createPresentation(listOf(vc!!.encode()), did, expirationDate = null).toCredential() as VerifiablePresentation

    val siopResponse = OIDC4VPService.getSIOPResponseFor(siopReqMod, did, listOf(presentation))
    val result = OIDC4VPService.postSIOPResponse(siopReqMod, siopResponse, CompatibilityMode.EBSI_WCT)
    (JSONParser(-1).parse(result) as JSONObject).get("result") shouldBe true
  }

}
