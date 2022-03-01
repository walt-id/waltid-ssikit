package id.walt.services.essif

import com.beust.klaxon.Klaxon
import id.walt.crypto.KeyAlgorithm
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.model.oidc.CredentialClaim
import id.walt.model.oidc.OIDCProvider
import id.walt.model.oidc.SIOPv2Request
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.essif.didebsi.DidEbsiService
import id.walt.services.essif.didebsi.EBSI_ENV_URL
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
import io.kotest.extensions.system.SystemEnvironmentTestListener
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

    val issuer = OIDCProvider("ebsi wct issuer", "$EBSI_WCT_ENV/conformance/v1/issuer-mock")

    val redirectUri = issuer.ciSvc.executeGetAuthorizationRequest(URI.create(REDIRECT_URI), listOf(CredentialClaim(type = SCHEMA_ID, manifest_id = null)), nonce = NONCE, state = STATE)
    redirectUri shouldNotBe null

    val code = OIDCUtils.getCodeFromRedirectUri(redirectUri!!)
    code shouldNotBe null

    val tokenResponse = issuer.ciSvc.getAccessToken(code!!, REDIRECT_URI, CompatibilityMode.EBSI_WCT)
    tokenResponse.indicatesSuccess() shouldBe true

    val proof = issuer.ciSvc.generateDidProof(did, tokenResponse.customParameters["c_nonce"]?.toString())
    vc = issuer.ciSvc.getCredential(tokenResponse.toSuccessResponse().oidcTokens.accessToken, did, SCHEMA_ID, proof, mode = CompatibilityMode.EBSI_WCT)
    vc shouldNotBe null
    vc?.credentialSchema shouldNotBe null
    vc?.credentialSchema?.id shouldBe SCHEMA_ID
  }

  @Test
  fun testVerificationFlow() {
    vc shouldNotBe null

    val verifier = OIDCProvider("ebsi wct issuer", "$EBSI_WCT_ENV/conformance/v1/verifier-mock")

    val siopReq = verifier.vpSvc.fetchSIOPv2Request()
    siopReq shouldNotBe null

    val redirectUri = URI.create(siopReq!!.redirect_uri)
    val ebsiEnvOverride = URI.create(EBSI_WCT_ENV)
    val siopReqMod = SIOPv2Request(
      redirect_uri = URI(ebsiEnvOverride.scheme, ebsiEnvOverride.authority, redirectUri.path, redirectUri.query, redirectUri.fragment).toString(),
      response_mode = siopReq!!.response_mode,
      nonce = siopReq!!.nonce,
      claims = siopReq!!.claims,
      state = siopReq!!.state
    )

    val presentation = Custodian.getService().createPresentation(listOf(vc!!.encode()), did, expirationDate = null).toCredential() as VerifiablePresentation

    val siopResponse = verifier.vpSvc.getSIOPResponseFor(siopReqMod!!, did, listOf(presentation))
    val result = verifier.vpSvc.postSIOPResponse(siopReqMod, siopResponse, CompatibilityMode.EBSI_WCT)
    (JSONParser(-1).parse(result) as JSONObject).get("result") shouldBe true
  }

}
