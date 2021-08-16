package id.walt.services.vc

import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.SignatureType
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.key.KeyService
import id.walt.signatory.ProofConfig
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.vclist.Europass
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.*
import io.kotest.matchers.collections.shouldContain
import java.nio.file.Files
import java.nio.file.Path
import java.text.ParseException
import java.text.SimpleDateFormat

class EbsiVCServiceTest : AnnotationSpec() {

    companion object {
        private val ebsiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

    init {
        ServiceMatrix("service-matrix.properties")
    }

    private val keyId = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
    private val issuerDid = DidService.create(DidMethod.ebsi, keyId.id)
    private val verificationMethod = "$issuerDid#${keyId.id}"
    private val proofPurpose = "assertionMethod"

    val vcReq = Europass(
        id = "education#higherEducation#51e42fda-cb0a-4333-b6a6-35cb147e1a88",
        issuer = issuerDid,
        issuanceDate = "2020-11-03T00:00:00Z",
        credentialSubject = Europass.CredentialSubject(
            id = "did:ebsi:22AhtW7XMssv7es4YcQTdV2MCM3c8b1VsiBfi5weHsjcCY9o",
        )
    ).encode()

    private val vc = WaltIdJwtCredentialService().sign(
        vcReq, ProofConfig(issuerDid = issuerDid, issuerVerificationMethod = verificationMethod, proofPurpose = proofPurpose)
    )

    @AfterAll
    fun tearDown() {
        val didFilename = "${issuerDid.replace(":", "-")}.json"
        Files.delete(Path.of("data", "did", "created", didFilename))
        KeyService.getService().delete(keyId.id)
    }

//    @Ignore
//    @Test
    fun testSignedVcAttributes() {
        // Verify the verifiable credential content
        val credential = vc.toCredential() as Europass
        credential.id shouldNotBe null
        credential.issuer shouldNotBe null
        credential.issuanceDate shouldNotBe null
        credential.credentialSubject!!.id shouldNotBe null
        credential.proof shouldNotBe null

        // Verify the verifiable credential proof
        val proof = credential.proof!!
        proof.type shouldBe SignatureType.EcdsaSecp256k1Signature2019.name
        isEbsiDate(proof.created!!) shouldBe true
        proof.proofPurpose shouldBe proofPurpose
        proof.verificationMethod shouldBe verificationMethod
        proof.jws shouldNotBe null

        // Verify the jwt claims
        val claims = JwtService.getService().parseClaims(proof.jws!!)!!
        claims["jti"] shouldBe credential.id!!
        claims["iss"] shouldBe credential.issuer!!
        claims["iat"] shouldBe ebsiFormat.parse(credential.issuanceDate!!)
        claims["nbf"] shouldBe ebsiFormat.parse(credential.issuanceDate!!)
        claims["exp"] shouldBe null
        claims["sub"] shouldBe credential.credentialSubject!!.id!!
        claims["vc"] shouldNotBe null

        // Verify "vc" claim content
        val vc = claims["vc"] as Map<*, *>
        vc.keys.size shouldBe 3
        vc.keys.forEach { listOf("credentialSubject", "type", "@context") shouldContain it }
        vc["@context"] shouldBe listOf("https://www.w3.org/2018/credentials/v1")
        vc["type"] shouldBe listOf("VerifiableCredential", "VerifiableAttestation", "Europass")
        vc["credentialSubject"] shouldNotBe null
    }

//    @Ignore
//    @Test
    fun testVerifyVc() =
        WaltIdJwtCredentialService().verifyVc(vc) shouldBe true

//    @Ignore
//    @Test
    fun testVerifyVcWithIssuerDid() =
        WaltIdJwtCredentialService().verifyVc(issuerDid, vc) shouldBe true

//    @Ignore
//    @Test
    fun testVerifyVcWithWrongIssuerDid() =
        WaltIdJwtCredentialService().verifyVc("wrong", vc) shouldBe false

    private fun isEbsiDate(value: String) =
        try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(value)
            true
        } catch (e: ParseException) {
            false
        }
}
