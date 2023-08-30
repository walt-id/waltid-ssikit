package id.walt.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import id.walt.auditor.PolicyRegistry
import id.walt.credentials.w3c.toPresentableCredential
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.test.RESOURCES_PATH
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.string.shouldContain
import java.io.File

class VcVerifyCommandTest : StringSpec({

    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")

    "vc verify --help" {
        val e = shouldThrow<PrintHelpMessage> {
            VerifyVcCommand().parse(listOf("--help"))
        }
        val message = e.context?.command?.getFormattedHelp()
        message shouldContain "Verify VC or VP"
    }

    "vc policies" {
        ListVerificationPoliciesCommand().parse(listOf())
    }

    "vc verify -p SignaturePolicy path/to/vp.json" {
        val did = DidService.create(DidMethod.key)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id
        val vcStr = Signatory.getService().issue(
            "VerifiableDiploma", ProofConfig(issuerDid = did, subjectDid = did, issuerVerificationMethod = vm)
        )
        val vpStr = JsonLdCredentialService.getService()
            .present(listOf(vcStr.toPresentableCredential()), did, "https://api-pilot.ebsi.eu", "d04442d3-661f-411e-a80f-42f19f594c9d", null)
        val vpFile = File.createTempFile("vpr", ".json")
        try {
            vpFile.writeText(vpStr)
            VerifyVcCommand().parse(listOf("-p", PolicyRegistry.defaultPolicyId, vpFile.absolutePath))
        } finally {
            vpFile.delete()
        }
    }

    "vc verify -p SignaturePolicy path/to/vp.jwt" {
        val did = DidService.create(DidMethod.key)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id
        val vcJwt = Signatory.getService().issue(
            "VerifiableDiploma",
            ProofConfig(issuerDid = did, subjectDid = did, issuerVerificationMethod = vm, proofType = ProofType.JWT)
        )
        val vpJwt = Custodian.getService().createPresentation(listOf(vcJwt.toPresentableCredential()), did, did, null, "abcd", null)
        val vpFile = File.createTempFile("vpr", ".jwt")
        try {
            vpFile.writeText(vpJwt)
            VerifyVcCommand().parse(listOf("-p", PolicyRegistry.defaultPolicyId, vpFile.absolutePath))
        } finally {
            vpFile.delete()
        }
    }

    "dynamic policies management CLI" {
        PolicyRegistry.listPolicyInfo().map { it.id } shouldNotContain "TestPolicy"
        CreateDynamicVerificationPolicyCommand().parse(
            listOf(
                "-n",
                "TestPolicy",
                "-p",
                "src/test/resources/rego/subject-policy.rego"
            )
        )
        PolicyRegistry.listPolicyInfo().map { it.id } shouldContain "TestPolicy"
        RemoveDynamicVerificationPolicyCommand().parse(listOf("-n", "TestPolicy"))
        PolicyRegistry.listPolicyInfo().map { it.id } shouldNotContain "TestPolicy"
    }
})
