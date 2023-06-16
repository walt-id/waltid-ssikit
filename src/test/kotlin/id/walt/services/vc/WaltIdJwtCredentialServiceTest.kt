package id.walt.services.vc

import com.nimbusds.jwt.SignedJWT
import id.walt.auditor.Auditor
import id.walt.auditor.policies.SignaturePolicy
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.credentials.w3c.schema.SchemaValidatorFactory
import id.walt.credentials.w3c.toPresentableCredential
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.credentials.w3c.toVerifiablePresentation
import id.walt.crypto.KeyAlgorithm
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.sdjwt.SDJwt
import id.walt.sdjwt.SDMap
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidEbsiCreateOptions
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
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContainAnyOf
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
        every { SchemaValidatorFactory.get(URI.create("https://api-pilot.ebsi.eu/trusted-schemas-registry/v1/schemas/0xb77f8516a965631b4f197ad54c65a9e2f9936ebfb76bae4906d33744dbcc60ba")) }.returns(
            SchemaValidatorFactory.get(
                URI.create("https://raw.githubusercontent.com/walt-id/waltid-ssikit-vclib/master/src/test/resources/schemas/VerifiableId.json")
                    .toURL().readText()
            )
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

        credentialService.validateSchemaTsr(noSchemaVc).isSuccess shouldBe true
        credentialService.validateSchemaTsr(validVc).isSuccess shouldBe true
        credentialService.validateSchemaTsr(invalidDataVc).isSuccess shouldBe false
        credentialService.validateSchemaTsr(notParsableVc).isSuccess shouldBe false
    }

    @Test
    fun testJwtWithDidEbsiV2() {
        val didV2 = DidService.create(DidMethod.ebsi, options = DidEbsiCreateOptions(version = 2))
        // issue credential using did ebsi v2
        val vc = Signatory.getService()
            .issue("VerifiableId", ProofConfig(didV2, didV2, proofType = ProofType.JWT, ecosystem = Ecosystem.ESSIF))
        VerifiableCredential.isJWT(vc) shouldBe true
        val signedVcJwt = SignedJWT.parse(vc)
        // verify jwk header is set
        signedVcJwt.header.jwk shouldNotBe null
        // create presentation using did ebsi v2
        val presentation = Custodian.getService().createPresentation(listOf(vc.toPresentableCredential()), didV2)
        VerifiableCredential.isJWT(presentation) shouldBe true

        val signedPresentationJwt = SignedJWT.parse(presentation)
        // verify jwk header is set
        signedPresentationJwt.header.jwk shouldNotBe null
        // remove key, to test key resolution from jwk header
        KeyService.getService().delete(didV2)
        KeyService.getService().hasKey(didV2) shouldBe false
        // verify presentation JWT
        val verificationResult = Auditor.getService().verify(presentation, listOf(SignaturePolicy()))
        verificationResult.result shouldBe true
        // verify key has been resolved
        KeyService.getService().hasKey(didV2) shouldBe true
    }

    @Test
    fun testSelectiveDisclosureJWTCredentials() {
        // issue VerifiableId with selectively disclosable:
        //  * credentialSubject
        //  * credentialSubject.firstName
        //  * credentialSubject.dateOfBirth

        val issuedVID = Signatory.getService()
            .issue(
                "VerifiableId", ProofConfig(
                    issuerDid = issuerDid,
                    subjectDid = issuerDid,
                    proofType = ProofType.SD_JWT,
                    selectiveDisclosure = SDMap.generateSDMap(
                        listOf("credentialSubject", "credentialSubject.firstName", "credentialSubject.dateOfBirth")
                    )
                )
            )

        val parsedSdJwt = SDJwt.parse(issuedVID)
        parsedSdJwt.disclosures shouldHaveSize 3
        parsedSdJwt.disclosureObjects.map { sd -> sd.key } shouldContainAll setOf("credentialSubject", "firstName", "dateOfBirth")

        Auditor.getService().verify(issuedVID, listOf(SignaturePolicy())).result shouldBe true

        val parsedIssuedVID = VerifiableCredential.fromString(issuedVID)
        parsedIssuedVID.credentialSubject shouldNotBe null
        parsedIssuedVID.credentialSubject!!.properties.keys shouldContainAll setOf("firstName", "dateOfBirth")

        // present vid with:
        //  1) disclose none,
        //  2) disclose credentialSubject (no nested sd props),
        //  3) disclose credentialSubject + 1 nested sd prop,
        //  4) disclose all

        val presentation1 = Custodian.getService().createPresentation(listOf(
            issuedVID.toPresentableCredential(discloseAll = false)
        ), issuerDid)
        val presentedvid1 = presentation1.toVerifiablePresentation().verifiableCredential!!.first()
        presentedvid1.credentialSubject shouldBe null
        Auditor.getService().verify(presentation1, listOf(SignaturePolicy())).result shouldBe true

        val presentation2 = Custodian.getService().createPresentation(listOf(
            issuedVID.toPresentableCredential(SDMap.generateSDMap(setOf("credentialSubject")))
        ), issuerDid)
        val presentedvid2 = presentation2.toVerifiablePresentation().verifiableCredential!!.first()
        presentedvid2.credentialSubject shouldNotBe null
        presentedvid2.credentialSubject!!.properties.keys shouldNotContainAnyOf setOf("firstName", "dateOfBirth")
        Auditor.getService().verify(presentation2, listOf(SignaturePolicy())).result shouldBe true

        val presentation3 = Custodian.getService().createPresentation(listOf(
            issuedVID.toPresentableCredential(SDMap.generateSDMap(setOf("credentialSubject", "credentialSubject.firstName")))
        ), issuerDid)
        val presentedvid3 = presentation3.toVerifiablePresentation().verifiableCredential!!.first()
        presentedvid3.credentialSubject shouldNotBe null
        presentedvid3.credentialSubject!!.properties.keys shouldNotContainAnyOf setOf("dateOfBirth")
        presentedvid3.credentialSubject!!.properties.keys shouldContainAll setOf("firstName")
        Auditor.getService().verify(presentation3, listOf(SignaturePolicy())).result shouldBe true

        val presentation4 = Custodian.getService().createPresentation(listOf(
            issuedVID.toPresentableCredential(discloseAll = true)
        ), issuerDid)
        val presentedvid4 = presentation4.toVerifiablePresentation().verifiableCredential!!.first()
        presentedvid4.credentialSubject shouldNotBe null
        presentedvid4.credentialSubject!!.properties.keys shouldContainAll setOf("firstName", "dateOfBirth")
        Auditor.getService().verify(presentation4, listOf(SignaturePolicy())).result shouldBe true
    }
}
