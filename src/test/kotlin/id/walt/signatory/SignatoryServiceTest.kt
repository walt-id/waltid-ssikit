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
    val didDoc = DidService.load(did)
    val vm = didDoc.verificationMethod!!.first().id

    "VerifiableId ld-proof" {

        val vc = signatory.issue(
            "VerifiableId", ProofConfig(
                subjectDid = did,
                issuerDid = did,
                issueDate = LocalDateTime.of(2020, 11, 3, 0, 0).toInstant(ZoneOffset.UTC),
                issuerVerificationMethod = vm
            )
        )

        println(vc)

        vc shouldContain "VerifiableId"
        vc shouldContain "0904008084H"
        vc shouldContain "Jane DOE"
        (vc.toCredential() as VerifiableId).issued shouldBe "2020-11-03T00:00:00Z"

        JsonLdCredentialService.getService().verify(vc).verified shouldBe true
    }

    "VerifiableId jwt-proof" {
        val jwtStr = signatory.issue(
            "VerifiableId",
            ProofConfig(subjectDid = did, issuerDid = did, proofType = ProofType.JWT)
        )

        println(jwtStr)

        val jwt = SignedJWT.parse(jwtStr)

        println(jwt.serialize())

        "EdDSA" shouldBe jwt.header.algorithm.name
        did shouldBe jwt.header.keyID
        did shouldBe jwt.jwtClaimsSet.claims["iss"]
        did shouldBe jwt.jwtClaimsSet.claims["sub"]

        JwtService.getService().verify(jwtStr) shouldBe true
    }

    "VerifiableDiploma ld-proof" {
        val vc = signatory.issue(
            "VerifiableDiploma", ProofConfig(
                subjectDid = did,
                issuerDid = did,
                issueDate = LocalDateTime.of(2020, 11, 3, 0, 0).toInstant(ZoneOffset.UTC),
                issuerVerificationMethod = vm
            )
        )

        println(vc)

        vc shouldContain "VerifiableDiploma"
        vc shouldContain "Leaston University"
        vc shouldContain "MASTERS LAW, ECONOMICS AND MANAGEMENT"
        (vc.toCredential() as VerifiableDiploma).issued shouldBe "2020-11-03T00:00:00Z"

        JsonLdCredentialService.getService().verify(vc).verified shouldBe true
    }

    "VerifiableDiploma jwt-proof" {
        val jwtStr = signatory.issue(
            "VerifiableDiploma", ProofConfig(subjectDid = did, issuerDid = did, proofType = ProofType.JWT)
        )

        println(jwtStr)

        val jwt = SignedJWT.parse(jwtStr)

        println(jwt.serialize())

        "EdDSA" shouldBe jwt.header.algorithm.name
        did shouldBe jwt.header.keyID
        did shouldBe jwt.jwtClaimsSet.claims["iss"]
        did shouldBe jwt.jwtClaimsSet.claims["sub"]

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
