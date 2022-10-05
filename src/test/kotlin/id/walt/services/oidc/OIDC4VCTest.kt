package id.walt.services.oidc

import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.oauth2.sdk.util.URIUtils
import com.nimbusds.oauth2.sdk.util.URLUtils
import com.nimbusds.openid.connect.sdk.Nonce
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.model.dif.*
import id.walt.model.oidc.*
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.test.RESOURCES_PATH
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.toCredential
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.instanceOf
import org.eclipse.jetty.util.URIUtil
import java.net.URI

class OIDC4VCTest : AnnotationSpec() {

    val TEST_ISSUER_PORT = 9000
    lateinit var testProvider: OIDCProviderWithMetadata
    val redirectUri = URI.create("http://blank")
    lateinit var SUBJECT_DID: String

    val FULL_AUTH_INITIATION_REQUEST = URI.create("openid-initiate-issuance:/?" +
        "issuer=http%3A%2F%2Flocalhost%3A$TEST_ISSUER_PORT" +
        "&credential_type=${OIDCTestProvider.TEST_CREDENTIAL_ID}" +
        "&op_state=${OIDCTestProvider.TEST_OP_STATE}")
    val PRE_AUTH_INITIATION_REQUEST = URI.create("openid-initiate-issuance:/?" +
        "issuer=http%3A%2F%2Flocalhost%3A$TEST_ISSUER_PORT" +
        "&credential_type=${OIDCTestProvider.TEST_CREDENTIAL_ID}" +
        "&pre-authorized_code=${OIDCTestProvider.TEST_PREAUTHZ_CODE}")


    @BeforeAll
    fun init() {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
        SUBJECT_DID = DidService.create(DidMethod.key)
        OIDCTestProvider.start(TEST_ISSUER_PORT)
        testProvider = OIDC4CIService.getWithProviderMetadata(OIDCProvider("test provider", "http://localhost:$TEST_ISSUER_PORT"))
    }

    @Test
    fun testIssuerPAR() {
        val uri = OIDC4CIService.executePushedAuthorizationRequest(testProvider, redirectUri, listOf(
            CredentialAuthorizationDetails(OIDCTestProvider.TEST_CREDENTIAL_ID, OIDCTestProvider.TEST_CREDENTIAL_FORMAT)
        ))
        uri shouldNotBe null
        uri!!.query shouldContain "request_uri=${OIDCTestProvider.TEST_REQUEST_URI}"
    }

    @Test
    fun testIssuerToken() {
        val tokenResponse = OIDC4CIService.getAccessToken(testProvider, OIDCTestProvider.TEST_AUTH_CODE, redirectUri.toString())
        tokenResponse.customParameters["c_nonce"] shouldBe OIDCTestProvider.TEST_NONCE
        tokenResponse.oidcTokens.accessToken.toString() shouldBe OIDCTestProvider.TEST_ACCESS_TOKEN
    }

    @Test
    fun testIssuerTokenPreauthz() {
        val tokenResponse = OIDC4CIService.getAccessToken(testProvider, OIDCTestProvider.TEST_PREAUTHZ_CODE, redirectUri.toString(), isPreAuthorized = true)
        tokenResponse.customParameters["c_nonce"] shouldBe OIDCTestProvider.TEST_NONCE
        tokenResponse.oidcTokens.accessToken.toString() shouldBe OIDCTestProvider.TEST_ACCESS_TOKEN
    }

    @Test
    fun testFullAuthIssuanceInitiationRequest() {
        val issuanceInitiationReq = IssuanceInitiationRequest.fromQueryParams(URLUtils.parseParameters(FULL_AUTH_INITIATION_REQUEST.query))
        issuanceInitiationReq.isPreAuthorized shouldBe false
        issuanceInitiationReq.op_state shouldNotBe null
        val issuer = OIDC4CIService.getWithProviderMetadata(OIDCProvider("issuer", issuanceInitiationReq.issuer_url))
        issuer.oidc_provider_metadata.pushedAuthorizationRequestEndpointURI shouldBe testProvider.oidc_provider_metadata.pushedAuthorizationRequestEndpointURI
        val uri = OIDC4CIService.executePushedAuthorizationRequest(issuer, redirectUri,
            issuanceInitiationReq.credential_types.map { CredentialAuthorizationDetails(it, OIDCTestProvider.TEST_CREDENTIAL_FORMAT) },
            op_state = issuanceInitiationReq.op_state)
        uri shouldNotBe null
        uri!!.query shouldContain "request_uri=${OIDCTestProvider.TEST_REQUEST_URI}"
    }

    @Test
    fun testPreAuthIssuanceInitiationRequest() {
        val issuanceInitiationReq = IssuanceInitiationRequest.fromQueryParams(URLUtils.parseParameters(PRE_AUTH_INITIATION_REQUEST.query))
        issuanceInitiationReq.isPreAuthorized shouldBe true
        issuanceInitiationReq.op_state shouldBe null
        val issuer = OIDC4CIService.getWithProviderMetadata(OIDCProvider("issuer", issuanceInitiationReq.issuer_url))
        issuer.oidc_provider_metadata.tokenEndpointURI shouldBe testProvider.oidc_provider_metadata.tokenEndpointURI
        val tokens = OIDC4CIService.getAccessToken(issuer, issuanceInitiationReq.pre_authorized_code!!, redirectUri.toString(), isPreAuthorized = true)
        tokens.oidcTokens.accessToken.value shouldBe OIDCTestProvider.TEST_ACCESS_TOKEN
    }

    @Test
    fun testParseNGIPreAuthIssuanceInitiationRequest() {
        // https://ngi-oidc4vci-test.spruceid.xyz/
        val reqUri = URI.create("openid-initiate-issuance://?" +
            "issuer=https%3A%2F%2Fngi%2Doidc4vci%2Dtest%2Espruceid%2Exyz&" +
            "credential_type=OpenBadgeCredential&" +
            "pre-authorized_code=eyJhbGciOiJFZERTQSJ9.eyJjcmVkZW50aWFsX3R5cGUiOlsiT3BlbkJhZGdlQ3JlZGVudGlhbCJdLCJleHAiOiIyMDIyLTEwLTA1VDExOjQ1OjQxLjk1NzM0MDYxNVoiLCJub25jZSI6IlFZMm15MDVKWHJPczd1Szg4OUVZSk1CSktkaXBnUXp0In0.f_-BNsLrL2LVTNxAjfJzX33pwC2zQDPGBMrY5LK88zdytOSRdyDfceat5Uzdb3MG3JNUEXEvLUoHYkgx95UCDQ")
        val issuanceInitiationReq = IssuanceInitiationRequest.fromQueryParams(URLUtils.parseParameters(reqUri.query))
        issuanceInitiationReq.isPreAuthorized shouldBe true
        issuanceInitiationReq.issuer_url shouldBe "https://ngi-oidc4vci-test.spruceid.xyz"
        issuanceInitiationReq.credential_types shouldContain "OpenBadgeCredential"
        issuanceInitiationReq.pre_authorized_code shouldBe "eyJhbGciOiJFZERTQSJ9.eyJjcmVkZW50aWFsX3R5cGUiOlsiT3BlbkJhZGdlQ3JlZGVudGlhbCJdLCJleHAiOiIyMDIyLTEwLTA1VDExOjQ1OjQxLjk1NzM0MDYxNVoiLCJub25jZSI6IlFZMm15MDVKWHJPczd1Szg4OUVZSk1CSktkaXBnUXp0In0.f_-BNsLrL2LVTNxAjfJzX33pwC2zQDPGBMrY5LK88zdytOSRdyDfceat5Uzdb3MG3JNUEXEvLUoHYkgx95UCDQ"
    }

    @Test
    fun testIssuerCredential() {
        val credential = OIDC4CIService.getCredential(testProvider,
            BearerAccessToken(OIDCTestProvider.TEST_ACCESS_TOKEN),
            OIDCTestProvider.TEST_CREDENTIAL_ID,
            OIDC4CIService.generateDidProof(testProvider, SUBJECT_DID, OIDCTestProvider.TEST_NONCE),
            OIDCTestProvider.TEST_CREDENTIAL_FORMAT)
        credential shouldNotBe null
        credential!!.type.last() shouldBe OIDCTestProvider.TEST_CREDENTIAL_ID
        credential.subject shouldBe SUBJECT_DID
        credential.issuer shouldBe OIDCTestProvider.ISSUER_DID
    }

    @Test
    fun testVerifyPresentation() {
        val credential = Signatory.getService().issue("VerifiableId", ProofConfig(OIDCTestProvider.ISSUER_DID, SUBJECT_DID))
        val presentation = Custodian.getService().createPresentation(listOf(credential), SUBJECT_DID, expirationDate = null).toCredential() as VerifiablePresentation
        val req = OIDC4VPService.createOIDC4VPRequest(
            URI("openid:///"),
            redirect_uri = URI.create("${testProvider.url}/present"),
            nonce = Nonce(),
            presentation_definition = OIDCTestProvider.TEST_PRESENTATION_DEFINITION
        )
        val resp = OIDC4VPService.getSIOPResponseFor(req, SUBJECT_DID, listOf(presentation))

        resp.presentation_submission?.definition_id shouldBe OIDCTestProvider.TEST_PRESENTATION_DEFINITION.id
        resp.presentation_submission?.descriptor_map?.get(0)?.path shouldBe "$"
        resp.presentation_submission?.descriptor_map?.get(0)?.format shouldBe "ldp_vp"
        resp.presentation_submission?.descriptor_map?.get(0)?.path_nested?.path shouldBe "$.verifiableCredential[0]"
        resp.presentation_submission?.descriptor_map?.get(0)?.path_nested?.format shouldBe "ldp_vc"

        val result = OIDC4VPService.postSIOPResponse(req, resp)
        result.trim() shouldBe resp.toFormBody() // test service returns siop response
    }

    @Test
    fun testVerifyJWTPresentation() {
        val credential = Signatory.getService().issue("VerifiableId", ProofConfig(OIDCTestProvider.ISSUER_DID, SUBJECT_DID, proofType = ProofType.JWT))
        val presentation = Custodian.getService().createPresentation(listOf(credential), SUBJECT_DID, expirationDate = null).toCredential() as VerifiablePresentation
        val req = OIDC4VPService.createOIDC4VPRequest(
            URI.create("openid:///"),
            redirect_uri = URI.create("${testProvider.url}/present"),
            nonce = Nonce(),
            presentation_definition = OIDCTestProvider.TEST_PRESENTATION_DEFINITION
        )
        val resp = OIDC4VPService.getSIOPResponseFor(req, SUBJECT_DID, listOf(presentation))

        resp.presentation_submission?.definition_id shouldBe OIDCTestProvider.TEST_PRESENTATION_DEFINITION.id
        resp.presentation_submission?.descriptor_map?.get(0)?.path shouldBe "$"
        resp.presentation_submission?.descriptor_map?.get(0)?.format shouldBe "jwt_vp"
        resp.presentation_submission?.descriptor_map?.get(0)?.path_nested?.path shouldBe "$.verifiableCredential[0]"
        resp.presentation_submission?.descriptor_map?.get(0)?.path_nested?.format shouldBe "jwt_vc"

        val result = OIDC4VPService.postSIOPResponse(req, resp)
        result.trim() shouldBe resp.toFormBody() // test service returns siop response
    }

    @Test
    fun testVerifyMultiplePresentations() {
        val credential = Signatory.getService().issue("VerifiableId", ProofConfig(OIDCTestProvider.ISSUER_DID, SUBJECT_DID))
        val presentation = Custodian.getService().createPresentation(listOf(credential), SUBJECT_DID, expirationDate = null).toCredential() as VerifiablePresentation
        val req = OIDC4VPService.createOIDC4VPRequest(
            URI.create("openid:///"),
            redirect_uri = URI.create("${testProvider.url}/present"),
            nonce = Nonce(),
            presentation_definition = OIDCTestProvider.TEST_PRESENTATION_DEFINITION
        )
        val resp = OIDC4VPService.getSIOPResponseFor(req, SUBJECT_DID, listOf(presentation, presentation))

        resp.presentation_submission?.definition_id shouldBe OIDCTestProvider.TEST_PRESENTATION_DEFINITION.id
        resp.presentation_submission?.descriptor_map!! shouldHaveSize 2
        resp.presentation_submission?.descriptor_map?.get(0)?.path shouldBe "$[0]"
        resp.presentation_submission?.descriptor_map?.get(1)?.path shouldBe "$[1]"
        resp.presentation_submission?.descriptor_map?.get(0)?.format shouldBe "ldp_vp"
        resp.presentation_submission?.descriptor_map?.get(1)?.format shouldBe "ldp_vp"
        resp.presentation_submission?.descriptor_map?.get(0)?.path_nested?.path shouldBe "$[0].verifiableCredential[0]"
        resp.presentation_submission?.descriptor_map?.get(0)?.path_nested?.format shouldBe "ldp_vc"
        resp.presentation_submission?.descriptor_map?.get(1)?.path_nested?.path shouldBe "$[1].verifiableCredential[0]"
        resp.presentation_submission?.descriptor_map?.get(1)?.path_nested?.format shouldBe "ldp_vc"

        val result = OIDC4VPService.postSIOPResponse(req, resp)
        result.trim() shouldBe resp.toFormBody() // test service returns siop response
    }

    @Test
    fun testVerifyMultipleJWTPresentations() {
        val credential = Signatory.getService().issue("VerifiableId", ProofConfig(OIDCTestProvider.ISSUER_DID, SUBJECT_DID, proofType = ProofType.JWT))
        val presentation = Custodian.getService().createPresentation(listOf(credential), SUBJECT_DID, expirationDate = null).toCredential() as VerifiablePresentation
        val req = OIDC4VPService.createOIDC4VPRequest(
            URI.create("openid:///"),
            redirect_uri = URI.create("${testProvider.url}/present"),
            nonce = Nonce(),
            presentation_definition = OIDCTestProvider.TEST_PRESENTATION_DEFINITION
        )
        val resp = OIDC4VPService.getSIOPResponseFor(req, SUBJECT_DID, listOf(presentation, presentation))

        resp.presentation_submission?.definition_id shouldBe OIDCTestProvider.TEST_PRESENTATION_DEFINITION.id
        resp.presentation_submission?.descriptor_map!! shouldHaveSize 2
        resp.presentation_submission?.descriptor_map?.get(0)?.path shouldBe "$[0]"
        resp.presentation_submission?.descriptor_map?.get(1)?.path shouldBe "$[1]"
        resp.presentation_submission?.descriptor_map?.get(0)?.format shouldBe "jwt_vp"
        resp.presentation_submission?.descriptor_map?.get(1)?.format shouldBe "jwt_vp"
        resp.presentation_submission?.descriptor_map?.get(0)?.path_nested?.path shouldBe "$[0].verifiableCredential[0]"
        resp.presentation_submission?.descriptor_map?.get(0)?.path_nested?.format shouldBe "jwt_vc"
        resp.presentation_submission?.descriptor_map?.get(1)?.path_nested?.path shouldBe "$[1].verifiableCredential[0]"
        resp.presentation_submission?.descriptor_map?.get(1)?.path_nested?.format shouldBe "jwt_vc"

        val result = OIDC4VPService.postSIOPResponse(req, resp)
        result.trim() shouldBe resp.toFormBody() // test service returns siop response
    }

    @Test
    fun testEbsiAuthRequestParsing() {
        val reqUri = "openid://?response_type=id_token&client_id=https%3A%2F%2Fapi.conformance.intebsi.xyz%2Fconformance%2Fv1%2Fverifier-mock%2Fauthentication-responses&scope=ebsi%20conformance%20mock&nonce=0f41579f-f012-489f-9715-ffeb2c744c6c&request=eyJhbGciOiJFUzI1NksiLCJ0eXAiOiJKV1QiLCJraWQiOiJkaWQ6ZWJzaTp6MjFTcU1KNTNmWVM2OVZaUllUVUVEYWcvI2tleXMtMSJ9.eyJpYXQiOjE2NDQyMzUxNjcsImV4cCI6MTY0NDIzNTQ2NywiaXNzIjoiZGlkOmVic2k6ejIxU3FNSjUzZllTNjlWWlJZVFVFRGFnIiwic2NvcGUiOiJlYnNpIGNvbmZvcm1hbmNlIG1vY2siLCJyZXNwb25zZV90eXBlIjoiaWRfdG9rZW4iLCJyZXNwb25zZV9tb2RlIjoicG9zdCIsImNsaWVudF9pZCI6Imh0dHBzOi8vYXBpLmNvbmZvcm1hbmNlLmludGVic2kueHl6L2NvbmZvcm1hbmNlL3YxL3ZlcmlmaWVyLW1vY2svYXV0aGVudGljYXRpb24tcmVzcG9uc2VzIiwicmVkaXJlY3RfdXJpIjoiaHR0cHM6Ly9hcGkuY29uZm9ybWFuY2UuaW50ZWJzaS54eXovY29uZm9ybWFuY2UvdjEvdmVyaWZpZXItbW9jay9hdXRoZW50aWNhdGlvbi1yZXNwb25zZXMiLCJub25jZSI6IjBmNDE1NzlmLWYwMTItNDg5Zi05NzE1LWZmZWIyYzc0NGM2YyIsInJlZ2lzdHJhdGlvbiI6Im1vY2sgcmVnaXN0cmF0aW9uIiwiY2xhaW1zIjp7ImlkX3Rva2VuIjp7InZwX3Rva2VuIjp7InByZXNlbnRhdGlvbl9kZWZpbml0aW9uIjp7ImlucHV0X2Rlc2NyaXB0b3JzIjpbeyJzY2hlbWEiOnsidXJpIjoiaHR0cHM6Ly9hcGkucHJlcHJvZC5lYnNpLmV1L3RydXN0ZWQtc2NoZW1hcy1yZWdpc3RyeS92MS9zY2hlbWFzLzB4MTRiMDViOTIxM2RiZTdkMzQzZWMxZmUxZDNjOGM3MzlhM2YzZGM1YTU5YmFlNTVlYjM4ZmEwYzI5NTEyNGY0OSMifX1dfX19fX0.IsIKBZlcRf2vvEgwR2gxYeg79yefQ3F0vsboUAGl4bgRO1r_g1kmiN96aXXiCsOqkeoz6fe0KE_AxRE5p0o6FQ&claims=%7B%22id_token%22%3A%7B%22vp_token%22%3A%7B%22presentation_definition%22%3A%7B%22input_descriptors%22%3A%5B%7B%22schema%22%3A%7B%22uri%22%3A%22https%3A%2F%2Fapi.preprod.ebsi.eu%2Ftrusted-schemas-registry%2Fv1%2Fschemas%2F0x14b05b9213dbe7d343ec1fe1d3c8c739a3f3dc5a59bae55eb38fa0c295124f49%23%22%7D%7D%5D%7D%7D%7D%7D"

        val siopReq = OIDC4VPService.parseOIDC4VPRequestUri(URI.create(reqUri))

        println(siopReq.redirectionURI.toString())
        println(OIDCUtils.getVCClaims(siopReq).toJSONString())
    }

    @Test
    fun testWaltAuthRequestParsing() {
        val reqUri = "http://localhost:3000/CredentialRequest/?response_type=id_token&response_mode=form_post&client_id=http%3A%2F%2Flocalhost%3A4000%2Fverifier-api%2Fverify%2Fb80f6828-ea05-4122-937d-b643bf4e2444&redirect_uri=http%3A%2F%2Flocalhost%3A4000%2Fverifier-api%2Fverify%2Fb80f6828-ea05-4122-937d-b643bf4e2444&scope=openid&nonce=b80f6828-ea05-4122-937d-b643bf4e2444&registration=%7B%22client_name%22+%3A+%22Walt.id+Verifier+Portal%22%2C+%22client_purpose%22+%3A+%22Verification+of+0x2488fd38783d65e4fd46e7889eb113743334dbc772b05df382b8eadce763101b%22%2C+%22did_methods_supported%22+%3A+%5B%22did%3Aebsi%3A%22%5D%2C+%22logo_uri%22+%3A+null%2C+%22subject_identifier_types_supported%22+%3A+%5B%22did%22%5D%2C+%22tos_uri%22+%3A+null%2C+%22vp_formats%22+%3A+%7B%22jwt_vp%22+%3A+%7B%22alg%22+%3A+%5B%22EdDSA%22%2C+%22ES256K%22%5D%7D%2C+%22ldp_vp%22+%3A+%7B%22proof_type%22+%3A+%5B%22Ed25519Signature2018%22%5D%7D%7D%7D&exp=1644330535&iat=1644244135&claims=%7B%22vp_token%22+%3A+%7B%22presentation_definition%22+%3A+%7B%22id%22+%3A+%221%22%2C+%22input_descriptors%22+%3A+%5B%7B%22id%22+%3A+%221%22%2C+%22schema%22+%3A+%7B%22uri%22+%3A+%22https%3A%2F%2Fapi.preprod.ebsi.eu%2Ftrusted-schemas-registry%2Fv1%2Fschemas%2F0x2488fd38783d65e4fd46e7889eb113743334dbc772b05df382b8eadce763101b%22%7D%7D%5D%7D%7D%7D"
        val siopReq = OIDC4VPService.parseOIDC4VPRequestUri(URI.create(reqUri))

        println(siopReq!!.redirectionURI.toString())
        println(OIDCUtils.getVCClaims(siopReq).toJSONString())
    }

    @Test
    fun testVPTokenConversion() {
        val credentialJWT = Signatory.getService().issue("VerifiableId", ProofConfig(OIDCTestProvider.ISSUER_DID, SUBJECT_DID, proofType = ProofType.JWT))
        val presentationJWT = Custodian.getService().createPresentation(listOf(credentialJWT), SUBJECT_DID, expirationDate = null).toCredential() as VerifiablePresentation

        val vp_token_JWT = OIDCUtils.toVpToken(listOf(presentationJWT))
        val vp_token_JWT_multi = OIDCUtils.toVpToken(listOf(presentationJWT, presentationJWT))
        val presentationsJWT = OIDCUtils.fromVpToken(vp_token_JWT)
        val presentationsJWT_multi = OIDCUtils.fromVpToken(vp_token_JWT_multi)
        presentationsJWT shouldNotBe null
        presentationsJWT!![0].encode() shouldBe presentationJWT.encode()
        presentationsJWT_multi shouldNotBe null
        presentationsJWT_multi!! shouldHaveSize 2
        presentationsJWT_multi!![0].encode() shouldBe presentationJWT.encode()
        presentationsJWT_multi!![1].encode() shouldBe presentationJWT.encode()

        val credentialLD = Signatory.getService().issue("VerifiableId", ProofConfig(OIDCTestProvider.ISSUER_DID, SUBJECT_DID, proofType = ProofType.LD_PROOF))
        val presentationLD = Custodian.getService().createPresentation(listOf(credentialLD), SUBJECT_DID, expirationDate = null).toCredential() as VerifiablePresentation

        val vp_token_LD = OIDCUtils.toVpToken(listOf(presentationLD))
        val vp_token_LD_multi = OIDCUtils.toVpToken(listOf(presentationLD, presentationLD))
        val presentationsLD = OIDCUtils.fromVpToken(vp_token_LD)
        val presentationsLD_multi = OIDCUtils.fromVpToken(vp_token_LD_multi)
        presentationsLD shouldNotBe null
        presentationsLD!![0].encode() shouldEqualJson presentationLD.encode()
        presentationsLD_multi shouldNotBe null
        presentationsLD_multi!! shouldHaveSize 2
        presentationsLD_multi!![0].encode() shouldEqualJson presentationLD.encode()
        presentationsLD_multi!![1].encode() shouldEqualJson presentationLD.encode()

    }

    @Test
    fun testVCClaimParsing() {
        val claim = "{\"credentials\":[{\"vp_token\":{\"issuanceDate\":\"2022-03-01T11:59:27Z\",\"holder\":\"did:key:z6Mkmf1LnkKdiS4jiwa8YgrQXmV7kBW1p4enKEULKFZbULr1\",\"id\":\"urn:uuid:148489d3-ddf6-46b1-ab0e-3d1c04b685e8\",\"validFrom\":\"2022-03-01T11:59:27Z\",\"issued\":\"2022-03-01T11:59:27Z\",\"type\":[\"VerifiablePresentation\"],\"@context\":[\"https:\\/\\/www.w3.org\\/2018\\/credentials\\/v1\"],\"verifiableCredential\":[]},\"type\":\"https:\\/\\/raw.githubusercontent.com\\/walt-id\\/waltid-ssikit-vclib\\/master\\/src\\/test\\/resources\\/schemas\\/ParticipantCredential.json\"}]}"
        val authReq = AuthorizationRequest.Builder(URI.create("http://blank"), ClientID())
            .customParameter("claims", claim).build()
        val vcclaim = OIDCUtils.getVCClaims(authReq)
        vcclaim.credentials shouldNotBe null
        vcclaim.credentials!! shouldNot beEmpty()
        vcclaim.credentials!![0].vp_token shouldNotBe null
        vcclaim.credentials!![0].vp_token!! shouldNot beEmpty()
        vcclaim.credentials!![0].vp_token!![0] shouldBe instanceOf<VerifiablePresentation>()

        val claim2 = "{\"vp_token\" : {\"presentation_definition\" : {\"format\" : null, \"id\" : \"1\", \"input_descriptors\" : [{\"constraints\" : {\"fields\" : [{\"filter\" : {\"constant\": \"VerifiableId\"}, \"id\" : \"1\", \"path\" : [\"\$.type\"], \"purpose\" : null}]}, \"format\" : null, \"group\" : [\"A\"], \"id\" : \"0\", \"name\" : null, \"purpose\" : null, \"schema\" : null}], \"name\" : null, \"purpose\" : null, \"submission_requirements\" : [{\"count\" : null, \"from\" : \"A\", \"from_nested\" : null, \"max\" : null, \"min\" : null, \"name\" : null, \"purpose\" : null, \"rule\" : \"all\"}]}}}"
        val authReq2 = AuthorizationRequest.Builder(URI.create("http://blank"), ClientID())
            .customParameter("claims", claim2).build()
        val vcclaim2 = OIDCUtils.getVCClaims(authReq2)
        vcclaim2.vp_token shouldNotBe null
    }

    @Test
    fun testVCClaimParsingWhenMissing() {
        val authReq = AuthorizationRequest.Builder(URI.create("http://blank"), ClientID()).build()
        val vcclaim = OIDCUtils.getVCClaims(authReq)
        vcclaim.credentials shouldBe null
        vcclaim.vp_token shouldBe null
    }

    @Test
    fun testInputDescriptorMatching() {
        val credential = Signatory.getService().issue("VerifiableId", ProofConfig(OIDCTestProvider.ISSUER_DID, SUBJECT_DID, proofType = ProofType.JWT)).toCredential()
        Custodian.getService().storeCredential(credential.id!!, credential)

        val pd = PresentationDefinition(
            id = "1",
            input_descriptors = listOf(
                InputDescriptor(
                    id = "schema_pex_1_0",
                    schema = VCSchema(credential.credentialSchema!!.id)
                ),
                InputDescriptor(
                    id = "field_type_pattern",
                    constraints = InputDescriptorConstraints(listOf(
                        InputDescriptorField(path = listOf("\$.type"), filter = mapOf("pattern" to "VerifiableId|VerifiableFOO"))
                    ))
                ),
                InputDescriptor(
                    id = "field_schema_const",
                    constraints = InputDescriptorConstraints(listOf(
                        InputDescriptorField(path = listOf("\$.credentialSchema.id"), filter = mapOf("const" to credential.credentialSchema!!.id))
                    ))
                )
            )
        )

        val matches = OIDCUtils.findCredentialsFor(pd)
        matches.keys shouldContainAll setOf("schema_pex_1_0", "field_type_pattern", "field_schema_const")
        matches.values.shouldForAll { set -> set.contains(credential.id!!) }

        val matchesBySubject = OIDCUtils.findCredentialsFor(pd, SUBJECT_DID)
        matchesBySubject shouldBe matches

        val noMatchesBySubject = OIDCUtils.findCredentialsFor(pd, OIDCTestProvider.ISSUER_DID)
        noMatchesBySubject.values.shouldForAll { set -> set.shouldBeEmpty() }
    }
}
