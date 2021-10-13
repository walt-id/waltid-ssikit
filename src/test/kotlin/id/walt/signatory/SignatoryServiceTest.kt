package id.walt.signatory

import com.nimbusds.jwt.SignedJWT
import id.walt.custodian.CustodianService
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.context.WaltContext
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vcstore.VcStoreService
import id.walt.test.RESOURCES_PATH
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.vclist.VerifiableId
import id.walt.vclib.vclist.VerifiableDiploma
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beInstanceOf
import java.text.SimpleDateFormat

class SignatoryServiceTest : StringSpec({
    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    val signatory = Signatory.getService()

    val did = DidService.create(DidMethod.key)

    "VerifiableId ld-proof" {
        val vc = signatory.issue(
            "VerifiableId", ProofConfig(
                subjectDid = did,
                issuerDid = did,
                issueDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse("2020-11-03T00:00:00Z"),
                issuerVerificationMethod = "Ed25519Signature2018"
            )
        )

        println(vc)

        vc shouldContain "VerifiableId"
        vc shouldContain "0904008084H"
        vc shouldContain "Jane DOE"
        (vc.toCredential() as VerifiableId).issuanceDate shouldBe "2020-11-03T00:00:00Z"

        JsonLdCredentialService.getService().verifyVc(vc) shouldBe true
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
                issueDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse("2020-11-03T00:00:00Z"),
                issuerVerificationMethod = "Ed25519Signature2018"
            )
        )

        println(vc)

        vc shouldContain "VerifiableDiploma"
        vc shouldContain "Leaston University"
        vc shouldContain "MASTERS LAW, ECONOMICS AND MANAGEMENT"
        (vc.toCredential() as VerifiableDiploma).issuanceDate shouldBe "2020-11-03T00:00:00Z"

        JsonLdCredentialService.getService().verifyVc(vc) shouldBe true
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
        (vcObj as VerifiableId).id.isNullOrBlank() shouldNotBe true
        val cred = WaltContext.vcStore.getCredential(vcObj.id!!, "signatory")
        cred should beInstanceOf<VerifiableId>()
        (cred as VerifiableId).id shouldBe vcObj.id
    }
})
