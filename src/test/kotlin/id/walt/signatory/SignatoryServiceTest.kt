package id.walt.signatory

import com.nimbusds.jwt.SignedJWT
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.vclist.Europass
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.text.SimpleDateFormat

class SignatoryServiceTest : StringSpec({
    ServiceMatrix("service-matrix.properties")
    val signatory = Signatory.getService()

    val did = DidService.create(DidMethod.key)

    "Europass ld-proof" {
        val vc = signatory.issue(
            "Europass", ProofConfig(
                subjectDid = did,
                issuerDid = did,
                issueDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse("2020-11-03T00:00:00Z"),
                issuerVerificationMethod = "Ed25519Signature2018"
            )
        )

        println(vc)

        vc shouldContain "Europass"
        vc shouldContain "Université de Lille"
        vc shouldContain "MASTERS LAW, ECONOMICS AND MANAGEMENT"
        (vc.toCredential() as Europass).issuanceDate shouldBe "2020-11-03T00:00:00Z"

        JsonLdCredentialService.getService().verifyVc(vc) shouldBe true
    }

    "Europass jwt-proof" {
        val jwtStr = signatory.issue(
            "Europass", ProofConfig(
                subjectDid = did,
                issuerDid = did,
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

        jwt.jwtClaimsSet.claims["vc"].let {
            it as Map<*, *>
            it.keys shouldNotContainAnyOf listOf("id", "issuer", "issuanceDate", "expirationDate")
            (it["credentialSubject"] as Map<*, *>).keys shouldNotContain "id"
        }

        JwtService.getService().verify(jwtStr) shouldBe true
    }
})
