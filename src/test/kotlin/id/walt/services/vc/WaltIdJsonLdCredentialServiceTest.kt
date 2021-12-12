package id.walt.services.vc

import com.beust.klaxon.Klaxon
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.WaltIdServices
import id.walt.services.did.DidService
import id.walt.signatory.DataProviderRegistry
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.test.DummySignatoryDataProvider
import id.walt.test.RESOURCES_PATH
import id.walt.test.getTemplate
import id.walt.test.readCredOffer
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.VcUtils
import id.walt.vclib.model.CredentialSchema
import id.walt.vclib.model.CredentialStatus
import id.walt.vclib.credentials.*
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.slf4j.event.Level
import java.io.File

class WaltIdJsonLdCredentialServiceTest : AnnotationSpec() {

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    private val credentialService = JsonLdCredentialService.getService()
    private val issuerWebDid = DidService.create(DidMethod.web)
    private val issuerKeyDid = DidService.create(DidMethod.key)
    private val subjectKeyDid = DidService.create(DidMethod.key)
    private val anotherKeyDid = DidService.create(DidMethod.key)
    private val issuerEbsiDid = DidService.create(DidMethod.ebsi)

    val VC_PATH = "src/test/resources/verifiable-credentials"

    fun genericSignVerify(issuerDid: String, credOffer: String) {

        val vcStr = credentialService.sign(credOffer, ProofConfig(issuerDid = issuerDid))
        println("Credential generated: $vcStr")

        val vc = vcStr.toCredential()
        println("Credential decoded: $vc")

        println("Verifying...")

        val vcVerified = credentialService.verify(vcStr)

        vcVerified.verified shouldBe true

        vcVerified.verificationType shouldBe VerificationType.VERIFIABLE_CREDENTIAL

        val holderDid = VcUtils.getSubject(vc)
        val vpStr = credentialService.present(listOf(vcStr), holderDid, "domain.com", "nonce")
        println("Presentation generated: $vpStr")

        val vp = vpStr.toCredential() as VerifiablePresentation
        println(vpStr)
        vp.proof?.domain shouldBe "domain.com"
        vp.proof?.nonce shouldBe "nonce"
        vp.proof?.proofPurpose shouldBe "authentication"
        vp.proof?.verificationMethod shouldBe DidService.resolve(holderDid).authentication?.get(0)

        val vpVerified = credentialService.verify(vpStr)
        vpVerified.verified shouldBe true
        VerificationType.VERIFIABLE_PRESENTATION shouldBe vpVerified.verificationType
    }

    @Test
    fun signEbsiVerifiableAttestation() {
        val template = getTemplate("ebsi-attestation") as VerifiableAttestation

        template.issuer = issuerWebDid
        template.credentialSubject!!.id = issuerWebDid // self signed

        val credOffer = Klaxon().toJsonString(template)

        genericSignVerify(issuerWebDid, credOffer)
    }

    @Test
    fun signEuropass() {
        val template = getTemplate("europass") as Europass

        template.issuer = issuerKeyDid
        template.credentialSubject!!.id = issuerKeyDid // self signed
        template.credentialSubject!!.learningAchievement!!.title = "Some Europass specific title"

        val credOffer = Klaxon().toJsonString(template)

        println("GENERIC SIGN VERIFY")
        genericSignVerify(issuerKeyDid, credOffer)
    }

    @Test
    fun signPermanentResidentCard() {
        val template = getTemplate("permanent-resident-card") as PermanentResidentCard

        template.issuer = issuerKeyDid
        template.credentialSubject!!.id = issuerKeyDid // self signed
        template.credentialSubject!!.givenName = "Given Name"

        val credOffer = Klaxon().toJsonString(template)

        genericSignVerify(issuerKeyDid, credOffer)
    }

    // TODO: create DID for holder @Test
    fun presentVa() {
        val vaStr = File("$VC_PATH/vc-ebsi-verifiable-authorisation.json").readText()

        val vp = credentialService.present(listOf(vaStr), VcUtils.getSubject(vaStr.toCredential()), null, null)

        println(vp)
    }

    @Test
    fun presentEuropassTest() {

        val domain = "example.com"
        val challenge = "asdf"

        val template = Europass(
            id = "education#higherEducation#51e42fda-cb0a-4333-b6a6-35cb147e1a88",
            issuer = "did:ebsi:2LGKvDMrNUPR6FhSNrXzQQ1h295zr4HwoX9UqvwAsenSKHe9",
            issuanceDate = "2020-11-03T00:00:00Z",
            validFrom = "2020-11-03T00:00:00Z",
            credentialSubject = Europass.CredentialSubject(
                id = "did:ebsi:22AhtW7XMssv7es4YcQTdV2MCM3c8b1VsiBfi5weHsjcCY9o",
                identifier = "0904008084H",
                givenNames = "Jane",
                familyName = "DOE",
                dateOfBirth = "1993-04-08T00:00:00Z",
                gradingScheme = Europass.CredentialSubject.GradingScheme(
                    id = "https://leaston.bcdiploma.com/law-economics-management#GradingScheme",
                    title = "Lower Second-Class Honours"
                ),
                learningAchievement = Europass.CredentialSubject.LearningAchievement(
                    id = "https://leaston.bcdiploma.com/law-economics-management#LearningAchievment",
                    title = "MASTERS LAW, ECONOMICS AND MANAGEMENT",
                    description = "MARKETING AND SALES",
                    additionalNote = listOf(
                        "DISTRIBUTION MANAGEMENT"
                    )
                ),
                awardingOpportunity = Europass.CredentialSubject.AwardingOpportunity(
                    id = "https://leaston.bcdiploma.com/law-economics-management#AwardingOpportunity",
                    identifier = "https://certificate-demo.bcdiploma.com/check/87ED2F2270E6C41456E94B86B9D9115B4E35BCCAD200A49B846592C14F79C86BV1Fnbllta0NZTnJkR3lDWlRmTDlSRUJEVFZISmNmYzJhUU5sZUJ5Z2FJSHpWbmZZ",
                    awardingBody = Europass.CredentialSubject.AwardingOpportunity.AwardingBody(
                        id = "did:ebsi:2LGKvDMrNUPR6FhSNrXzQQ1h295zr4HwoX9UqvwAsenSKHe9",
                        eidasLegalIdentifier = "Unknown",
                        registration = "0597065J",
                        preferredName = "Leaston University",
                        homepage = "https://leaston.bcdiploma.com/"
                    ),
                    location = "FRANCE",
                    startedAtTime = "2015-11-03T00:00:00Z",
                    endedAtTime = "2020-11-03T00:00:00Z"
                ),
                learningSpecification = Europass.CredentialSubject.LearningSpecification(
                    id = "https://leaston.bcdiploma.com/law-economics-management#LearningSpecification",
                    ISCEDFCode = listOf(
                        "7"
                    ),
                    ECTSCreditPoints = 120,
                    EQFLevel = 7,
                    NQFLevel = listOf(
                        "7"
                    )
                )
            ),
            credentialStatus = CredentialStatus(
                id = "https://essif.europa.eu/status/education#higherEducation#51e42fda-cb0a-4333-b6a6-35cb147e1a88",
                type = "CredentialsStatusList2020"
            ),
            credentialSchema = CredentialSchema(
                id = "https://essif.europa.eu/trusted-schemas-registry/v1/schemas/to_be_obtained_after_registration_of_the_schema",
                type = "JsonSchemaValidator2018"
            )
        )

        template.issuer = issuerEbsiDid
        template.credentialSubject!!.id = subjectKeyDid
        template.credentialSubject!!.learningAchievement!!.title = "Some Europass specific title"

        val vc = credentialService.sign(template.encode() , ProofConfig(issuerDid = issuerEbsiDid))

        println("Signed vc: $vc")
        val vcSigned = vc.toCredential()
        println(vcSigned.toString())

        val vp = credentialService.present(listOf(vc), VcUtils.getSubject(vcSigned), domain, challenge)
        println("Presentation generated: $vp")

        val vpVerified = credentialService.verifyVp(vp)
        vpVerified shouldBe true

    }


    // TODO: consider methods below, as old data-model might be used
    @Test
    fun signCredentialECDSASecp256k1Test() {

        val credOffer = readCredOffer("WorkHistory")

        val domain = "example.com"
        val nonce: String? = null

        val vc = credentialService.sign(credOffer, ProofConfig(issuerDid = issuerWebDid, nonce = nonce, domain = domain))
        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verifyVc(issuerWebDid, vc)
        vcVerified shouldBe true
    }

    @Test
    fun signCredentialEd25519k1Test() {

        val credOffer = readCredOffer("WorkHistory")

        val domain = "example.com"
        val nonce: String? = null

        val vc = credentialService.sign(credOffer, ProofConfig(issuerDid = issuerKeyDid, nonce = nonce, domain = domain))
        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verifyVc(issuerKeyDid, vc)
        vcVerified shouldBe true
    }

    @Test
    fun signEuropassCredentialTest() {

        val credOffer = readCredOffer("VerifiableAttestation-Europass")

        val domain = "example.com"
        val nonce: String? = null

        val vc = credentialService.sign(credOffer, ProofConfig(issuerDid = issuerKeyDid, nonce = nonce, domain = domain))
        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verifyVc(issuerKeyDid, vc)
        vcVerified shouldBe true
    }


    @Test
    fun signCredentialWrongValidationKeyTest() {

        val credOffer = readCredOffer("WorkHistory")

        val vc = credentialService.sign(credOffer, ProofConfig(issuerDid = issuerKeyDid))

        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verifyVc(anotherKeyDid, vc)
        vcVerified shouldBe false
    }

    @Test
    fun testValidateSchemaTsr() {
        // Required at the moment because EBSI did not upgrade V_ID schema with necessary changes.
        DataProviderRegistry.register(VerifiableId::class, DummySignatoryDataProvider())

        val noSchemaVc = VerifiableId().encode()
        val validVc = Signatory.getService().issue("VerifiableId", ProofConfig(
            issuerDid = issuerKeyDid,
            subjectDid = subjectKeyDid,
            proofPurpose = "testing",
            issuerVerificationMethod = "testing",
            proofType = ProofType.LD_PROOF))
        val invalidDataVc = Signatory.getService().issue("VerifiableId", ProofConfig(
            issuerDid = issuerKeyDid,
            proofType = ProofType.LD_PROOF))
        val notParsableVc = ""

        credentialService.validateSchemaTsr(noSchemaVc) shouldBe false
        credentialService.validateSchemaTsr(validVc) shouldBe true
        credentialService.validateSchemaTsr(invalidDataVc) shouldBe false
        credentialService.validateSchemaTsr(notParsableVc) shouldBe false
    }

/*@Test
fun signCredentialInvalidDataTest() {

    val credOffer = readCredOffer("vc-offer-simple-example")
    val issuerDid = DidService.create(DidMethod.key)

    val vcStr = credentialService.sign(issuerDid, credOffer)
    println("Credential generated: $vcStr")
    val vcInvalid = VcLibManager.getVerifiableCredential(vcStr)
    vcInvalid.id = "INVALID ID"
    val vcInvalidStr = vcInvalid.encode()
    println("Credential generated: ${vcInvalidStr}")

    val vcVerified = credentialService.verifyVc(issuerDid, vcInvalidStr)
    vcVerified shouldBe false
}*/

// TODO @Test
/*fun presentValidCredentialTest() {

    val credOffer = Klaxon().parse<VerifiableCredential>(readCredOffer("vc-offer-simple-example"))
    val issuerDid = DidService.create(DidMethod.web)
    val subjectDid = DidService.create(DidMethod.key)

    credOffer.id = Timestamp.valueOf(LocalDateTime.now()).time.toString()
    credOffer.issuer = issuerDid
    credOffer.credentialSubject.id = subjectDid

    credOffer.issuanceDate = LocalDateTime.now()

    val vcReqEnc = Klaxon().toJsonString(credOffer)

    println("Credential request:\n$vcReqEnc")

    val vcStr = credentialService.sign(issuerDid, vcReqEnc)
    val vc = Klaxon().parse<VerifiableCredential>(vcStr)
    println("Credential generated: ${vc.encodePretty()}")

    val vpIn = VerifiablePresentation(
        listOf("https://www.w3.org/2018/credentials/v1"),
        "id",
        listOf("VerifiablePresentation"),
        listOf(vc),
        null
    )
    val vpInputStr = Klaxon().toJsonString(vpIn)

    val domain = "example.com"
    val nonce: String? = "asdf"
    val vp = credentialService.sign(issuerDid, vpInputStr, domain, nonce)
    vp shouldNotBe null
    println("Verifiable Presentation generated: $vp")

    var ret = credentialService.verifyVp(vp)
    assertTrue { ret }
}

//TODO @Test
fun presentInvalidCredentialTest() {
    val issuerDid = DidService.create(DidMethod.web)
    val subjectDid = DidService.create(DidMethod.key)

    val credOffer = Klaxon().parse<VerifiableCredential>(readCredOffer("vc-offer-simple-example"))
    //      credOffer.id = Timestamp.valueOf(LocalDateTime.now()).time.toString() // This line is causing LD-Signatures to fail (will produces a valid signature, although the data is invalidated below)
    credOffer.issuer = issuerDid
    credOffer.credentialSubject.id = subjectDid

    credOffer.issuanceDate = LocalDateTime.now()

    //val vcReqEnc = readCredOffer("vc-offer-simple-example") -> produces false-signature for invalid credential
    val vcReqEnc = Json {
        prettyPrint = true
    }.encodeToString(credOffer) // FIXME does not produce false-signature for invalid credential

    println("Credential request:\n$vcReqEnc")

    val vcStr = credentialService.sign(issuerDid, vcReqEnc)
    println("Credential generated: $vcStr")
    val vcInvalid = Klaxon().parse<VerifiableCredential>(vcStr)
    vcInvalid.credentialSubject.id = "INVALID ID"
    val vcInvalidStr = vcInvalid.encodePretty()
    println("Credential generated: ${vcInvalidStr}")

    val vcValid = credentialService.verifyVc(issuerDid, vcInvalidStr)
    vcValid shouldBe false

    val vcVerified = credentialService.verifyVc(issuerDid, vcInvalidStr)

    val vpIn = VerifiablePresentation(
        listOf("https://www.w3.org/2018/credentials/v1"),
        "id",
        listOf("VerifiablePresentation"),
        listOf(vcInvalid),
        null
    )
    val vpInputStr = Klaxon().toJsonString(vpIn)

    print(vpInputStr)

    val domain = "example.com"
    val nonce: String? = "asdf"
    val vp = credentialService.sign(issuerDid, vpInputStr, domain, nonce)
    vp shouldNotBe null
    println("Verifiable Presentation generated: $vp")

    var ret = credentialService.verifyVp(vp)
    ret shouldBe false
}*/
}
