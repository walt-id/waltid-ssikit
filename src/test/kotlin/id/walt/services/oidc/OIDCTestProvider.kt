package id.walt.services.oidc

import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.http.ServletUtils
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.oauth2.sdk.token.RefreshToken
import com.nimbusds.openid.connect.sdk.Nonce
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse
import com.nimbusds.openid.connect.sdk.token.OIDCTokens
import id.walt.auditor.Auditor
import id.walt.auditor.SignaturePolicy
import id.walt.model.DidMethod
import id.walt.model.dif.InputDescriptor
import id.walt.model.dif.PresentationDefinition
import id.walt.model.dif.VpSchema
import id.walt.model.oidc.*
import id.walt.services.did.DidService
import id.walt.signatory.ProofConfig
import id.walt.signatory.Signatory
import id.walt.vclib.model.Proof
import id.walt.vclib.templates.VcTemplateManager
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.HttpCode
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldNotBe
import java.net.URI
import io.javalin.apibuilder.ApiBuilder.*
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import java.nio.charset.StandardCharsets
import java.util.*

object OIDCTestProvider {

  val TEST_CREDENTIAL_CLAIM = CredentialClaim(type = VcTemplateManager.loadTemplate("VerifiableId").credentialSchema!!.id, manifest_id = null)
  val TEST_VP_CLAIM = VpTokenClaim(PresentationDefinition(listOf(InputDescriptor(VpSchema(uri = VcTemplateManager.loadTemplate("VerifiableId").credentialSchema!!.id))), id = "1"))
  val TEST_REQUEST_URI = "urn:ietf:params:oauth:request_uri:test"
  val TEST_AUTH_CODE = "testcode"
  val TEST_ACCESS_TOKEN = "testtoken"
  val TEST_NONCE = "testnonce"
  val TEST_ID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
  lateinit var ISSUER_DID: String

  fun testPar(ctx: Context) {
    val authReq = AuthorizationRequest.parse(ServletUtils.createHTTPRequest(ctx.req))
    val claims = OIDCUtils.getVCClaims(authReq)

    claims?.credentials shouldNotBe null
    claims!!.credentials!! shouldContain TEST_CREDENTIAL_CLAIM
    ctx.status(HttpCode.CREATED).json(PushedAuthorizationSuccessResponse(URI(TEST_REQUEST_URI), 3600).toJSONObject())
  }

  fun testToken(ctx: Context) {
    val tokenReq = TokenRequest.parse(ServletUtils.createHTTPRequest(ctx.req))
    tokenReq.authorizationGrant.type shouldBe GrantType.AUTHORIZATION_CODE
    (tokenReq.authorizationGrant as AuthorizationCodeGrant).authorizationCode.value shouldBe TEST_AUTH_CODE
    ctx.json(
      OIDCTokenResponse(OIDCTokens(TEST_ID_TOKEN, BearerAccessToken(TEST_ACCESS_TOKEN), RefreshToken()), mapOf("c_nonce" to TEST_NONCE)).toJSONObject())
  }

  fun testCredential(ctx: Context) {
    val did = ctx.formParam("did")
    did shouldNotBe null
    val type = ctx.formParam("type")
    type shouldBe TEST_CREDENTIAL_CLAIM.type
    val format = ctx.formParam("format")
    format shouldBe "ldp_vc"
    val proofStr = ctx.formParam("proof")
    proofStr shouldNotBe null
    val proof = klaxon.parse<Proof>(proofStr!!)
    proof shouldNotBe null
    proof!!.creator shouldBe did

    val credential = Signatory.getService().issue("VerifiableId", ProofConfig(ISSUER_DID, did))
    ctx.json(CredentialResponse(format, Base64.getUrlEncoder().encodeToString(credential.toByteArray(StandardCharsets.UTF_8))))
  }

  fun testPresent(ctx: Context) {
    val siopResponse = SIOPv2Response.fromFormParams(
      ctx.formParamMap().map { Pair(it.key, it.value.first()) }.toMap()
    )
    siopResponse shouldNotBe null
    siopResponse!!.id_token.verify() shouldBe true
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
