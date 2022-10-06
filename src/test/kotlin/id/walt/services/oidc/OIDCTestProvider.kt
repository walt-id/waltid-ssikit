package id.walt.services.oidc

import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.http.ServletUtils
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.oauth2.sdk.token.RefreshToken
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse
import com.nimbusds.openid.connect.sdk.token.OIDCTokens
import id.walt.auditor.Auditor
import id.walt.auditor.SignaturePolicy
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.model.dif.InputDescriptor
import id.walt.model.dif.PresentationDefinition
import id.walt.model.dif.VCSchema
import id.walt.model.oidc.*
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.vclib.model.Proof
import id.walt.vclib.model.toCredential
import id.walt.vclib.templates.VcTemplateManager
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.HttpCode
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldNotBe
import java.net.URI
import io.javalin.apibuilder.ApiBuilder.*
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.mockk.InternalPlatformDsl.toStr
import java.nio.charset.StandardCharsets
import java.util.*

object OIDCTestProvider {

  val TEST_CREDENTIAL_ID = "VerifiableId"
  val TEST_CREDENTIAL_FORMAT = "ldp_vc"
  val TEST_PRESENTATION_DEFINITION = PresentationDefinition("1", listOf(InputDescriptor("1", schema = VCSchema(uri = VcTemplateManager.loadTemplate("VerifiableId").credentialSchema!!.id))))
  val TEST_REQUEST_URI = "urn:ietf:params:oauth:request_uri:test"
  val TEST_AUTH_CODE = "testcode"
  val TEST_PREAUTHZ_CODE = "preauthcode"
  val TEST_ACCESS_TOKEN = "testtoken"
  val TEST_NONCE = "testnonce"
  val TEST_ID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
  val TEST_OP_STATE = "opstate"
  lateinit var ISSUER_DID: String

  fun testPar(ctx: Context) {
    val authReq = AuthorizationRequest.parse(ServletUtils.createHTTPRequest(ctx.req))
    if(authReq.customParameters.containsKey("op_state")) {
      authReq.customParameters["op_state"]!!.first() shouldBe TEST_OP_STATE
    }

    val credentialDetails = OIDC4CIService.getCredentialAuthorizationDetails(authReq)

    credentialDetails shouldNot beEmpty()
    credentialDetails.size shouldBe 1
    credentialDetails.first().credential_type shouldBe TEST_CREDENTIAL_ID
    credentialDetails.first().format shouldBe TEST_CREDENTIAL_FORMAT

    ctx.status(HttpCode.CREATED).json(PushedAuthorizationSuccessResponse(URI(TEST_REQUEST_URI), 3600).toJSONObject())
  }

  fun testToken(ctx: Context) {
    val tokenReq = TokenRequest.parse(ServletUtils.createHTTPRequest(ctx.req))
    tokenReq.authorizationGrant.type shouldBeIn listOf(GrantType.AUTHORIZATION_CODE, PreAuthorizedCodeGrant.GRANT_TYPE)

    if(tokenReq.authorizationGrant.type == GrantType.AUTHORIZATION_CODE)
      (tokenReq.authorizationGrant as AuthorizationCodeGrant).authorizationCode.value shouldBe TEST_AUTH_CODE
    else
      (tokenReq.authorizationGrant as PreAuthorizedCodeGrant).code.value shouldBe TEST_PREAUTHZ_CODE

    ctx.json(
      OIDCTokenResponse(OIDCTokens(TEST_ID_TOKEN, BearerAccessToken(TEST_ACCESS_TOKEN), RefreshToken()), mapOf("c_nonce" to TEST_NONCE)).toJSONObject())
  }

  fun testCredential(ctx: Context) {
    ctx.contentType() shouldBe "application/json"
    val credentialReq = klaxon.parse<CredentialRequest>(ctx.body())
    credentialReq shouldNotBe null
    credentialReq!!.format shouldBe TEST_CREDENTIAL_FORMAT
    credentialReq.type shouldBe TEST_CREDENTIAL_ID
    credentialReq.proof shouldNotBe null
    val jwt = JWTParser.parse(credentialReq.proof!!.jwt) as SignedJWT
    val kid = jwt.header.keyID?.toString()
    kid shouldNotBe null
    val did = DidUrl.from(kid!!).did
    val credential = Signatory.getService().issue("VerifiableId", ProofConfig(ISSUER_DID, did, proofType = if(credentialReq.format == "jwt_vc") ProofType.JWT else ProofType.LD_PROOF))
    ctx.json(klaxon.toJsonString(CredentialResponse(credentialReq.format, credential.toCredential())))
  }

  fun testPresent(ctx: Context) {
    val siopResponse = SIOPv2Response.fromFormParams(
      ctx.formParamMap().map { Pair(it.key, it.value.first()) }.toMap()
    )
    siopResponse shouldNotBe null
    siopResponse.vp_token shouldNot beEmpty()
    siopResponse.vp_token.forEach { vp ->
      Auditor.getService().verify(vp.encode(), listOf(SignaturePolicy())).valid shouldBe true
    }
    ctx.result(siopResponse.toFormBody())
  }

  fun testNonce(ctx: Context) {
    ctx.json(klaxon.toJsonString(NonceResponse(TEST_NONCE, "300")))
  }

  fun start(port: Int = 8000) {
    ISSUER_DID = DidService.create(DidMethod.key)
    Javalin.create().routes {
      post("par", OIDCTestProvider::testPar)
      post("token", OIDCTestProvider::testToken)
      post("credential", OIDCTestProvider::testCredential)
      post("present", OIDCTestProvider::testPresent)
      post("nonce", OIDCTestProvider::testNonce)
    }.start(port)
  }
}
