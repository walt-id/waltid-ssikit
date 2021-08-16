package id.walt.signatory

import com.nimbusds.jwt.SignedJWT
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class SignatoryServiceTest : StringSpec({
    ServiceMatrix("service-matrix.properties")
    val signatory = Signatory.getService()

    val did = DidService.create(DidMethod.key)

    "Europass ld-proof" {
        val vc = signatory.issue(
            "Europass", ProofConfig(
                subjectDid = did,
                issuerDid = did,
                issuerVerificationMethod = "Ed25519Signature2018"
            )
        )

        println(vc)

        vc shouldContain "Europass"
        vc shouldContain "Université de Lille"
        vc shouldContain "MASTERS LAW, ECONOMICS AND MANAGEMENT"

        JsonLdCredentialService.getService().verifyVc(vc) shouldBe true
    }

    "Europass jwt-proof" {
        val jwtStr = signatory.issue(
            "Europass", ProofConfig(
                subjectDid = did,
                issuerDid = did,
                issuerVerificationMethod = "Ed25519Signature2018",
                proofType = ProofType.JWT
            )
        )

        println(jwtStr)

        val jwt = SignedJWT.parse(jwtStr)

        println(jwt.serialize())

        "EdDSA" shouldBe jwt.header.algorithm.name
        did shouldBe jwt.header.keyID
        did shouldBe jwt.jwtClaimsSet.claims["iss"]
        did shouldBe jwt.jwtClaimsSet.claims["sub"]

        JwtService.getService().verify(jwtStr) shouldBe true

//        vc shouldContain "Europass"
//        vc shouldContain "Université de Lille"
//        vc shouldContain "ECONOMICS AND MANAGEMENT"
//
//        JwtCredentialService.getService().verifyVc(vc) shouldBe true
    }
})
