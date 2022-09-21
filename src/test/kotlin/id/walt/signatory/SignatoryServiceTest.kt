package id.walt.signatory

import com.nimbusds.jwt.SignedJWT
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.context.ContextManager
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.signatory.dataproviders.MergingDataProvider
import id.walt.test.RESOURCES_PATH
import id.walt.vclib.credentials.VerifiableDiploma
import id.walt.vclib.credentials.VerifiableId
import id.walt.vclib.model.toCredential
import id.walt.vclib.templates.VcTemplateManager
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beInstanceOf
import java.time.LocalDateTime
import java.time.ZoneOffset

class SignatoryServiceTest : StringSpec({
    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    val signatory = Signatory.getService()

    val did = DidService.create(DidMethod.key)

    "Issue and verify: VerifiableId (LD-Proof)" {
        println("ISSUING CREDENTIAL...")
        val vc = signatory.issue(
            "VerifiableId", ProofConfig(
                subjectDid = did,
                issuerDid = did,
                issueDate = LocalDateTime.of(2020, 11, 3, 0, 0).toInstant(ZoneOffset.UTC),
                issuerVerificationMethod = "Ed25519Signature2018"
            )
        )

        println("VC:")
        println(vc)

        println("Running Checks...")
        vc shouldContain "VerifiableId"
        vc shouldContain "0904008084H"
        vc shouldContain "Jane DOE"
        (vc.toCredential() as VerifiableId).issued shouldBe "2020-11-03T00:00:00Z"

        println("VERIFYING VC")
        JsonLdCredentialService.getService().verifyVc(vc) shouldBe true
    }

    "Issue and verify: VerifiableId (JWT-Proof)" {
        println("ISSUING CREDENTIAL...")
        val jwtStr = signatory.issue(
            "VerifiableId", ProofConfig(
                subjectDid = did,
                issuerDid = did,
                proofType = ProofType.JWT
            )
        )

        println("VC:")
        println(jwtStr)

        val jwt = SignedJWT.parse(jwtStr)

        println(jwt.serialize())

        println("Running Checks...")
        "EdDSA" shouldBe jwt.header.algorithm.name
        did shouldBe jwt.header.keyID
        did shouldBe jwt.jwtClaimsSet.claims["iss"]
        did shouldBe jwt.jwtClaimsSet.claims["sub"]

        println("VERIFYING VC")
        JwtService.getService().verify(jwtStr) shouldBe true
    }

    "Issue and verify: VerifiableDiploma (LD-Proof)" {
        println("ISSUING CREDENTIAL...")
        val vc = signatory.issue(
            "VerifiableDiploma", ProofConfig(
                subjectDid = did,
                issuerDid = did,
                issueDate = LocalDateTime.of(2020, 11, 3, 0, 0).toInstant(ZoneOffset.UTC),
                issuerVerificationMethod = "Ed25519Signature2018"
            )
        )

        println("VC:")
        println(vc)

        println("Running Checks...")
        vc shouldContain "VerifiableDiploma"
        vc shouldContain "Leaston University"
        vc shouldContain "MASTERS LAW, ECONOMICS AND MANAGEMENT"
        (vc.toCredential() as VerifiableDiploma).issued shouldBe "2020-11-03T00:00:00Z"

        println("VERIFYING VC")
        JsonLdCredentialService.getService().verifyVc(vc) shouldBe true
    }

    "Issue and verify: VerifiableDiploma (JWT-Proof)" {
        println("ISSUING CREDENTIAL...")
        val jwtStr = signatory.issue(
            "VerifiableDiploma", ProofConfig(subjectDid = did, issuerDid = did, proofType = ProofType.JWT)
        )

        println("VC:")
        println(jwtStr)

        val jwt = SignedJWT.parse(jwtStr)

        println(jwt.serialize())

        println("Running Checks...")
        "EdDSA" shouldBe jwt.header.algorithm.name
        did shouldBe jwt.header.keyID
        did shouldBe jwt.jwtClaimsSet.claims["iss"]
        did shouldBe jwt.jwtClaimsSet.claims["sub"]

        println("VERIFYING VC")
        JwtService.getService().verify(jwtStr) shouldBe true
    }

    "vc storage test" {
        val vc = signatory.issue("VerifiableId", ProofConfig(subjectDid = did, issuerDid = did, proofType = ProofType.LD_PROOF))
        val vcObj = vc.toCredential()
        vcObj should beInstanceOf<VerifiableId>()
        (vcObj as VerifiableId).id.isNullOrBlank() shouldBe false
        val cred = ContextManager.vcStore.getCredential(vcObj.id!!, "signatory")
        cred should beInstanceOf<VerifiableId>()
        (cred as VerifiableId).id shouldBe vcObj.id
    }

    "merging data provider" {
        val templ = VcTemplateManager.loadTemplate("VerifiableId")
        val data = mapOf(Pair("credentialSubject", mapOf(Pair("firstName", "Yves"))))
        val populated = MergingDataProvider(data).populate(templ, ProofConfig(subjectDid = did, issuerDid = did, proofType = ProofType.LD_PROOF))

        populated.javaClass shouldBe VerifiableId::class.java

        (populated as VerifiableId).credentialSubject?.firstName shouldBe "Yves"
        populated.credentialSubject?.id shouldBe did
        populated.issuer shouldBe did
    }
})
