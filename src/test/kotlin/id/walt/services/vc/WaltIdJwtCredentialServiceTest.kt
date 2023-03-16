package id.walt.services.vc

import com.nimbusds.jwt.SignedJWT
import id.walt.auditor.Auditor
import id.walt.auditor.SignaturePolicy
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.credentials.w3c.schema.SchemaValidatorFactory
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.crypto.KeyAlgorithm
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.key.KeyService
import id.walt.signatory.Ecosystem
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkObject
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.*

class WaltIdJwtCredentialServiceTest : AnnotationSpec() {

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    private val credentialService = JwtCredentialService.getService()
    private val jwtService = JwtService.getService()
    private val keyId = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)

    private val id = "education#higherEducation#51e42fda-cb0a-4333-b6a6-35cb147e1a88"
    private val issuerDid = DidService.create(DidMethod.ebsi, keyId.id)
    private val subjectDid = "did:ebsi:22AhtW7XMssv7es4YcQTdV2MCM3c8b1VsiBfi5weHsjcCY9o"
    private val issueDate = Instant.now()
    private val validDate = issueDate.minus(Duration.ofDays(1))
    private val expirationDate = issueDate.plus(Duration.ofDays(1))

    @BeforeAll
    fun setup() {
      mockkObject(SchemaValidatorFactory)
      every { SchemaValidatorFactory.get(URI.create("https://api-pilot.ebsi.eu/trusted-schemas-registry/v1/schemas/0xb77f8516a965631b4f197ad54c65a9e2f9936ebfb76bae4906d33744dbcc60ba"))}.returns(
        SchemaValidatorFactory.get(URI.create("https://raw.githubusercontent.com/walt-id/waltid-ssikit-vclib/master/src/test/resources/schemas/VerifiableId.json").toURL().readText())
      )
    }

    @AfterAll
    fun tearDown() {
        // Only required if file-key store is used
//        val didFilename = "${issuerDid.replace(":", "-")}.json"
//        Files.delete(Path.of("data", "did", "created", didFilename))
        KeyService.getService().delete(keyId.id)
    }

    @Test
    fun testSignedVcAttributes() {
        val credential = credentialService.sign(
            VerifiableCredential().encode(),
            ProofConfig(
                credentialId = id,
                issuerDid = issuerDid,
                subjectDid = subjectDid,
                issueDate = issueDate,
                validDate = validDate,
                expirationDate = expirationDate
            )
        )

        val claims = jwtService.parseClaims(credential)!!
        claims["jti"] shouldBe id
        claims["iss"] shouldBe issuerDid
        claims["sub"] shouldBe subjectDid
        (claims["iat"] as Date).toInstant().epochSecond shouldBe issueDate.epochSecond
        (claims["nbf"] as Date).toInstant().epochSecond shouldBe validDate.epochSecond
        (claims["exp"] as Date).toInstant().epochSecond shouldBe expirationDate.epochSecond
        claims shouldContainKey "vc"
        claims["vc"].let {
            it as Map<*, *>
            it.keys.size shouldBe 2
            it.keys.forEach { listOf("@context", "type") shouldContain it }
            it["@context"] shouldBe listOf("https://www.w3.org/2018/credentials/v1")
            it["type"] shouldBe listOf("VerifiableCredential")
        }
    }

    @Test
    fun testOptionalConfigsAreNull() {
        val claims = jwtService.parseClaims(
            credentialService.sign(VerifiableCredential().encode(), ProofConfig(issuerDid = issuerDid))
        )!!
        claims["jti"] shouldBe null
        claims["iss"] shouldBe issuerDid
        claims["sub"] shouldBe null
        claims["iat"] shouldNotBe null
        claims["nbf"] shouldNotBe null
        claims["exp"] shouldBe null
        claims shouldContainKey "vc"
    }


    @Test
    fun testVerifyVc() = credentialService.verifyVc(
        credentialService.sign(VerifiableCredential().encode(), ProofConfig(issuerDid = issuerDid))
    ) shouldBe true

    @Test
    fun testVerifyVcWithIssuerDid() = credentialService.verifyVc(
        issuerDid,
        credentialService.sign(VerifiableCredential().encode(), ProofConfig(issuerDid = issuerDid))
    ) shouldBe true

    @Test
    fun testVerifyVcWithWrongIssuerDid() = credentialService.verifyVc(
        "wrong",
        credentialService.sign(
            VerifiableCredential().encode(), ProofConfig(issuerDid = issuerDid)
        )
    ) shouldBe false

    @Test
    fun testValidateSchema() {
        val noSchemaVc = VerifiableCredential().encode()
        val validVc = Signatory.getService()
            .issue("VerifiableId", ProofConfig(issuerDid = issuerDid, subjectDid = issuerDid, proofType = ProofType.JWT))
        val invalidDataVc =
            Signatory.getService().issue(
                W3CCredentialBuilder().setCredentialSchema(validVc.toVerifiableCredential().credentialSchema!!)
                    .buildSubject { setProperty("foo", "bar") }, ProofConfig(issuerDid = issuerDid, proofType = ProofType.JWT)
            )
        val notParsableVc = ""

        credentialService.validateSchemaTsr(noSchemaVc).outcome shouldBe true
        credentialService.validateSchemaTsr(validVc).outcome shouldBe true
        credentialService.validateSchemaTsr(invalidDataVc).outcome shouldBe false
        credentialService.validateSchemaTsr(notParsableVc).outcome shouldBe false
    }

    @Test
    fun testJwtWithDidEbsiV2() {
        val didV2 = DidService.create(DidMethod.ebsi, options = DidService.DidEbsiOptions(version = 2))
        // issue credential using did ebsi v2
        val vc = Signatory.getService()
            .issue("VerifiableId", ProofConfig(didV2, didV2, proofType = ProofType.JWT, ecosystem = Ecosystem.ESSIF))
        VerifiableCredential.isJWT(vc) shouldBe true
        val signedVcJwt = SignedJWT.parse(vc)
        // verify jwk header is set
        signedVcJwt.header.jwk shouldNotBe null
        // create presentation using did ebsi v2
        val presentation = Custodian.getService().createPresentation(listOf(vc), didV2)
        VerifiableCredential.isJWT(presentation) shouldBe true

        val signedPresentationJwt = SignedJWT.parse(presentation)
        // verify jwk header is set
        signedPresentationJwt.header.jwk shouldNotBe null
        // remove key, to test key resolution from jwk header
        KeyService.getService().delete(didV2)
        KeyService.getService().hasKey(didV2) shouldBe false
        // verify presentation JWT
        val verificationResult = Auditor.getService().verify(presentation, listOf(SignaturePolicy()))
        verificationResult.outcome shouldBe true
        // verify key has been resolved
        KeyService.getService().hasKey(didV2) shouldBe true
    }
}
