package id.walt.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import id.walt.signatory.DataProviderRegistry
import id.walt.signatory.ProofConfig
import id.walt.signatory.SignatoryDataProvider
import id.walt.test.RESOURCES_PATH
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.VerifiableAttestation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain


class VcIssueCommandTest : StringSpec({

    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")

    val key = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
    var didIssuer = DidService.create(DidMethod.ebsi, keyAlias = key.id)
    var didSubject = DidService.create(DidMethod.key)

    DataProviderRegistry.register(VerifiableAttestation::class, object : SignatoryDataProvider {
        override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableAttestation =
            (template as VerifiableAttestation).apply {
                issuer = "NEW ISSUER"
                id = proofConfig.id ?: ""
            }
    })

    "vc issue --help" {
        val e = shouldThrow<PrintHelpMessage> {
            VcIssueCommand().parse(listOf("--help"))
        }
        val message = e.command.getFormattedHelp()
        message shouldContain "-t, --template"
        message shouldContain "-i, --issuer-did"
        message shouldContain "-s, --subject-did"
    }

    "vc issue default" {
        VcIssueCommand().parse(listOf("-i", didIssuer, "-s", didSubject))
    }

    "vc issue VerifiableId LD_PROOF" {
        VcIssueCommand().parse(listOf("-i", didIssuer, "-s", didSubject, "-t", "VerifiableId", "-p", "LD_PROOF"))
    }

    "vc issue VerifiableDiploma LD_PROOF" {
        VcIssueCommand().parse(listOf("-i", didIssuer, "-s", didSubject, "-t", "VerifiableDiploma", "-p", "LD_PROOF"))
    }

    "vc issue VerifiableDiploma LD_PROOF incl. issuerVerificationMethod " {
        val issuerVerificationMethod = DidService.loadDidEbsi(didIssuer).verificationMethod?.get(0)?.id!!
        VcIssueCommand().parse(listOf("-i", didIssuer, "-s", didSubject, "-t", "VerifiableDiploma", "-p", "LD_PROOF", "-v", issuerVerificationMethod))
    }

    "vc issue VerifiableAttestation LD_PROOF" {
        //VcIssueCommand().parse(listOf("-i", didIssuer, "-s", didSubject, "-t", "VerifiableAttestation"))
    }

    "vc issue VerifiableId JWT" {
        VcIssueCommand().parse(listOf("-i", didIssuer, "-s", didSubject, "-t", "VerifiableId", "-p", "JWT"))
    }

    "vc issue VerifiableDiploma JWT" {
        VcIssueCommand().parse(listOf("-i", didIssuer, "-s", didSubject, "-t", "VerifiableDiploma", "-p", "JWT"))
    }

})
