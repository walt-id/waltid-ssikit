package id.walt.services.vc

import com.beust.klaxon.Klaxon
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.WaltIdServices
import id.walt.services.did.DidService
import id.walt.services.essif.EssifServer.nonce
import id.walt.services.essif.TrustedIssuerClient.domain
import id.walt.signatory.ProofConfig
import id.walt.test.getTemplate
import id.walt.test.readCredOffer
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.model.CredentialSchema
import id.walt.vclib.model.CredentialStatus
import id.walt.vclib.vclist.Europass
import id.walt.vclib.vclist.PermanentResidentCard
import id.walt.vclib.vclist.VerifiableAttestation
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.apache.logging.log4j.Level
import java.io.File

class WaltIdJsonLdCredentialServiceTest : AnnotationSpec() {

    private val credentialService = JsonLdCredentialService.getService()


    val VC_PATH = "src/test/resources/verifiable-credentials"

    @Before
    fun setup() {
        ServiceMatrix("service-matrix.properties")
    }

    fun genericSignVerify(issuerDid: String, credOffer: String) {

        val vcStr = credentialService.sign(credOffer, ProofConfig(issuerDid = issuerDid))
        println("Credential generated: $vcStr")

        val vc = vcStr.toCredential()
        println("Credential decoded: $vc")
        WaltIdServices.setLogLevel(Level.DEBUG)

        println("Verifying...")

        val vcVerified = credentialService.verify(vcStr)

        vcVerified.verified shouldBe true

        vcVerified.verificationType shouldBe VerificationType.VERIFIABLE_CREDENTIAL

        val vpStr = credentialService.present(vcStr, "domain.com", "nonce")
        println("Presentation generated: $vpStr")

        // TODO FIX
//        val vp = VC.decode(vpStr)
//        println(vp)
//        "domain.com" shouldBe vp.proof?.domain
//        "nonce" shouldBe vp.proof?.nonce

        val vpVerified = credentialService.verify(vpStr)
        vpVerified.verified shouldBe true
        VerificationType.VERIFIABLE_PRESENTATION shouldBe vpVerified.verificationType
    }

    @Test
    fun signEbsiVerifiableAttestation() {
        val template = getTemplate("ebsi-attestation") as VerifiableAttestation

        val issuerDid = DidService.create(DidMethod.web)

        template.issuer = issuerDid
        template.credentialSubject!!.id = issuerDid // self signed

        val credOffer = Klaxon().toJsonString(template)

        genericSignVerify(issuerDid, credOffer)
    }

    @Test
    fun signEuropass() {
        val template = getTemplate("europass") as Europass

        val issuerDid = DidService.create(DidMethod.key)

        template.issuer = issuerDid
        template.credentialSubject!!.id = issuerDid // self signed
        template.credentialSubject!!.learningAchievement!!.title = "Some Europass specific title"

        val credOffer = Klaxon().toJsonString(template)

        println("GENERIC SIGN VERIFY")
        WaltIdServices.setLogLevel(Level.DEBUG)
        genericSignVerify(issuerDid, credOffer)
    }

    @Test
    fun signPermanentResidentCard() {
        val template = getTemplate("permanent-resident-card") as PermanentResidentCard

        val issuerDid = DidService.create(DidMethod.key)

        template.issuer = issuerDid
        template.credentialSubject!!.id = issuerDid // self signed
        template.credentialSubject!!.givenName = "Given Name"

        val credOffer = Klaxon().toJsonString(template)

        genericSignVerify(issuerDid, credOffer)
    }

    // TODO: create DID for holder @Test
    fun presentVa() {
        val vaStr = File("$VC_PATH/vc-ebsi-verifiable-authorisation.json").readText()

        val vp = credentialService.present(vaStr, null, null)

        println(vp)
    }

    @Test
    fun presentEuropassTest() {

        val issuerDid = DidService.create(DidMethod.ebsi)
        val subjectDid = DidService.create(DidMethod.key)
        val domain = "example.com"
        val challenge: String = "asdf"

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
                dateOfBirth = "1993-04-08",
                gradingScheme = Europass.CredentialSubject.GradingScheme(
                    id = "https://blockchain.univ-lille.fr/ontology#GradingScheme",
                    title = "Lower Second-Class Honours"
                ),
                learningAchievement = Europass.CredentialSubject.LearningAchievement(
                    id = "https://blockchain.univ-lille.fr/ontology#LearningAchievment",
                    title = "MASTERS LAW, ECONOMICS AND MANAGEMENT",
                    description = "MARKETING AND SALES",
                    additionalNote = listOf(
                        "DISTRIBUTION MANAGEMENT"
                    )
                ),
                awardingOpportunity = Europass.CredentialSubject.AwardingOpportunity(
                    id = "https://blockchain.univ-lille.fr/ontology#AwardingOpportunity",
                    identifier = "https://certificate-demo.bcdiploma.com/check/87ED2F2270E6C41456E94B86B9D9115B4E35BCCAD200A49B846592C14F79C86BV1Fnbllta0NZTnJkR3lDWlRmTDlSRUJEVFZISmNmYzJhUU5sZUJ5Z2FJSHpWbmZZ",
                    awardingBody = Europass.CredentialSubject.AwardingOpportunity.AwardingBody(
                        id = "did:ebsi:2LGKvDMrNUPR6FhSNrXzQQ1h295zr4HwoX9UqvwAsenSKHe9",
                        eidasLegalIdentifier = "Unknown",
                        registration = "0597065J",
                        preferredName = "Université de Lille",
                        homepage = "https://www.univ-lille.fr/"
                    ),
                    location = "FRANCE",
                    startedAtTime = "Unknown",
                    endedAtTime = "2020-11-03T00:00:00Z"
                ),
                learningSpecification = Europass.CredentialSubject.LearningSpecification(
                    id = "https://blockchain.univ-lille.fr/ontology#LearningSpecification",
                    iSCEDFCode = listOf(
                        "7"
                    ),
                    eCTSCreditPoints = 120,
                    eQFLevel = 7,
                    nQFLevel = listOf(
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

        template.issuer = issuerDid
        template.credentialSubject!!.id = subjectDid
        template.credentialSubject!!.learningAchievement!!.title = "Some Europass specific title"

        val vc = credentialService.sign(template.encode() , ProofConfig(issuerDid = issuerDid))

        println("Signed vc: $vc")
        val vcSigned = vc.toCredential()
        println(vcSigned.toString())

        val vp = credentialService.present(vc, domain, challenge)
        println("Presentation generated: $vp")

        val vpVerified = credentialService.verifyVp(vp)
        vpVerified shouldBe true

    }


    // TODO: consider methods below, as old data-model might be used
    @Test
    fun signCredentialECDSASecp256k1Test() {

        val credOffer = readCredOffer("WorkHistory")

        val issuerDid = DidService.create(DidMethod.web) // DID web uses an ECDSA Secp256k1
        val domain = "example.com"
        val nonce: String? = null

        val vc = credentialService.sign(credOffer, ProofConfig(issuerDid = issuerDid, nonce = nonce, domain = domain))
        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verifyVc(issuerDid, vc)
        vcVerified shouldBe true
    }

    @Test
    fun signCredentialEd25519k1Test() {

        val credOffer = readCredOffer("WorkHistory")

        val issuerDid = DidService.create(DidMethod.key) // DID key uses an EdDSA Ed25519k1 key
        val domain = "example.com"
        val nonce: String? = null

        val vc = credentialService.sign(credOffer, ProofConfig(issuerDid = issuerDid, nonce = nonce, domain = domain))
        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verifyVc(issuerDid, vc)
        vcVerified shouldBe true
    }

    @Test
    fun signEuropassCredentialTest() {

        val credOffer = readCredOffer("VerifiableAttestation-Europass")

        val issuerDid = DidService.create(DidMethod.key) // DID key uses an EdDSA Ed25519k1 key
        val domain = "example.com"
        val nonce: String? = null

        val vc = credentialService.sign(credOffer, ProofConfig(issuerDid = issuerDid, nonce = nonce, domain = domain))
        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verifyVc(issuerDid, vc)
        vcVerified shouldBe true
    }


    @Test
    fun signCredentialWrongValidationKeyTest() {

        val credOffer = readCredOffer("WorkHistory")

        val issuerDid = DidService.create(DidMethod.key)
        val anotherDid = DidService.create(DidMethod.key)

        val vc = credentialService.sign(credOffer, ProofConfig(issuerDid = issuerDid))

        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verifyVc(anotherDid, vc)
        vcVerified shouldBe false
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
