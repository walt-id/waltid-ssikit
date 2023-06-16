package id.walt.signatory

import com.nimbusds.jwt.SignedJWT
import id.walt.auditor.Auditor
import id.walt.auditor.policies.SignaturePolicy
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.W3CIssuer
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.context.ContextManager
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.signatory.dataproviders.MergingDataProvider
import id.walt.test.RESOURCES_PATH
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDateTime
import java.time.ZoneOffset

class SignatoryServiceTest : StringSpec({
    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    val signatory = Signatory.getService()

    val did = DidService.create(DidMethod.key)
    val didDoc = DidService.load(did)
    val vm = didDoc.verificationMethod!!.first().id

    "Issue and verify: VerifiableId (LD-Proof)" {
        println("ISSUING CREDENTIAL...")
        val vc = signatory.issue(
            "VerifiableId", ProofConfig(
                subjectDid = did,
                issuerDid = did,
                issueDate = LocalDateTime.of(2020, 11, 3, 0, 0).toInstant(ZoneOffset.UTC),
                issuerVerificationMethod = vm,
                proofType = ProofType.LD_PROOF
            )
        )

        println("VC:")
        println(vc)

        println("Running Checks...")
        vc shouldContain "VerifiableId"
        vc shouldContain "0904008084H"
        vc shouldContain "Jane DOE"
        vc.toVerifiableCredential().issued shouldBe "2020-11-03T00:00:00Z"

        JsonLdCredentialService.getService().verify(vc).verified shouldBe true
    }

    "Issue and verify: VerifiableId (JWT-Proof)" {
        println("ISSUING CREDENTIAL...")
        val jwtStr = signatory.issue(
            "VerifiableId", ProofConfig(
                subjectDid = did, issuerDid = did, proofType = ProofType.JWT
            )
        )

        println("VC:")
        println(jwtStr)

        val jwt = SignedJWT.parse(jwtStr)

        println(jwt.serialize())

        println("Running Checks...")
        "EdDSA" shouldBe jwt.header.algorithm.name
        vm shouldBe jwt.header.keyID
        did shouldBe jwt.jwtClaimsSet.claims["iss"]
        did shouldBe jwt.jwtClaimsSet.claims["sub"]

        println("VERIFYING VC")
        JwtService.getService().verify(jwtStr).verified shouldBe true
    }

    "Issue and verify: VerifiableDiploma (LD-Proof)" {
        println("ISSUING CREDENTIAL...")
        val vc = signatory.issue(
            "VerifiableDiploma", ProofConfig(
                subjectDid = did,
                issuerDid = did,
                issueDate = LocalDateTime.of(2020, 11, 3, 0, 0).toInstant(ZoneOffset.UTC),
                issuerVerificationMethod = vm,
                proofType = ProofType.LD_PROOF
            )
        )

        println("VC:")
        println(vc)

        println("Running Checks...")
        vc shouldContain "VerifiableDiploma"
        vc shouldContain "Leaston University"
        vc shouldContain "MASTERS LAW, ECONOMICS AND MANAGEMENT"
        vc.toVerifiableCredential().issued shouldBe "2020-11-03T00:00:00Z"

        JsonLdCredentialService.getService().verify(vc).verified shouldBe true
    }

    "Issue and verify: VerifiableDiploma (JWT-Proof)" {
        println("ISSUING CREDENTIAL...")
        val jwtStr = signatory.issue(
            "VerifiableDiploma",
            ProofConfig(subjectDid = did, issuerDid = did, proofType = ProofType.JWT)
        )

        println("VC:")
        println(jwtStr)

        val jwt = SignedJWT.parse(jwtStr)

        println(jwt.serialize())

        println("Running Checks...")
        "EdDSA" shouldBe jwt.header.algorithm.name
        vm shouldBe jwt.header.keyID
        did shouldBe jwt.jwtClaimsSet.claims["iss"]
        did shouldBe jwt.jwtClaimsSet.claims["sub"]

        println("VERIFYING VC")
        JwtService.getService().verify(jwtStr).verified shouldBe true
    }

    "vc storage test" {
        val vc = signatory.issue(
            "VerifiableId",
            ProofConfig(subjectDid = did, issuerDid = did, proofType = ProofType.LD_PROOF),
            storeCredential = true
        )
        val vcObj = vc.toVerifiableCredential()
        vcObj.type shouldContain "VerifiableId"
        vcObj.id.isNullOrBlank() shouldBe false
        val cred = ContextManager.vcStore.getCredential(vcObj.id!!, "signatory")
        cred?.id shouldBe vcObj.id
    }

    "merging data provider" {
        val builder = W3CCredentialBuilder()
        val data = mapOf(Pair("credentialSubject", mapOf(Pair("firstName", "Yves"))))
        val populated = MergingDataProvider(data).populate(
            builder, ProofConfig(subjectDid = did, issuerDid = did, proofType = ProofType.LD_PROOF)
        ).build()

        populated.credentialSubject?.properties?.get("firstName") shouldBe "Yves"
    }

    "sign any credential from template with user data from json" {
        val template = """
            {
                "type": [ "VerifiableCredential" ]
            }
        """.trimIndent()
        val userData = """
            {
                "credentialSubject": {
                    "firstName": "Inco",
                    "familyName": "GNITO"
                }
            }
        """.trimIndent()

        val signedVC = Signatory.getService().issue(
            W3CCredentialBuilder.fromPartial(template).setFromJson(userData),
            ProofConfig(subjectDid = did, issuerDid = did, proofType = ProofType.LD_PROOF)
        )

        println(signedVC)

        val parsedVC = VerifiableCredential.fromJson(signedVC)
        parsedVC.issuerId shouldBe did
        parsedVC.subjectId shouldBe did
        parsedVC.proof shouldNotBe null
        parsedVC.credentialSubject?.properties?.get("firstName") shouldBe "Inco"
        parsedVC.credentialSubject?.properties?.get("familyName") shouldBe "GNITO"

        signedVC shouldMatchJson parsedVC.toJson()
        Auditor.getService().verify(parsedVC, listOf(SignaturePolicy())).result shouldBe true
    }

    "sign any credential with user data from subject builder" {
        val signedVC = Signatory.getService().issue(
            W3CCredentialBuilder().buildSubject {
                setProperty("firstName", "Inco")
                setProperty("familyName", "GNITO")
            },
            ProofConfig(subjectDid = did, issuerDid = did, proofType = ProofType.LD_PROOF),
            W3CIssuer(did, mapOf("name" to "Test Issuer"))
        )

        println(signedVC)

        val parsedVC = VerifiableCredential.fromJson(signedVC)
        parsedVC.issuerId shouldBe did
        parsedVC.subjectId shouldBe did
        parsedVC.proof shouldNotBe null
        parsedVC.credentialSubject?.properties?.get("firstName") shouldBe "Inco"
        parsedVC.credentialSubject?.properties?.get("familyName") shouldBe "GNITO"
        parsedVC.issuer?.properties?.get("name") shouldBe "Test Issuer"

        signedVC shouldMatchJson parsedVC.toJson()
        Auditor.getService().verify(parsedVC, listOf(SignaturePolicy())).result shouldBe true
    }

    /*"sign verifiable id credential with typed builder" {
        val signedVC = Signatory.getService().issue(
            VerifiableIdBuilder()
                .setFirstName("Inco")
                .setFamilyName("GNITO"),
            ProofConfig(subjectDid = did, issuerDid = did, proofType = ProofType.LD_PROOF)
        )

        println(signedVC)

        val parsedVC = VerifiableIdCredential.fromJson(signedVC)
        parsedVC.issuer shouldBe did
        parsedVC.subject shouldBe did
        parsedVC.proof shouldNotBe null
        parsedVC.firstName shouldBe "Inco"
        parsedVC.familyName shouldBe "GNITO"

        signedVC shouldMatchJson parsedVC.toJson()
        Auditor.getService().verify(parsedVC, listOf(SignaturePolicy())).valid shouldBe true
    }*/
})
