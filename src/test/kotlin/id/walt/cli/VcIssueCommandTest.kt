package id.walt.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import id.walt.servicematrix.ServiceMatrix
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import id.walt.cli.EssifOnboardingCommand
import id.walt.cli.VcCommand
import id.walt.cli.VcIssueCommand
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import java.io.File


class VcIssueCommandTest : StringSpec({

    ServiceMatrix("service-matrix.properties")

    val key = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
    var didIssuer = DidService.create(DidMethod.ebsi, keyAlias = key.id)
    var didSubject = DidService.create(DidMethod.key)

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

    "vc issue Europass LD_PROOF" {
        VcIssueCommand().parse(listOf("-i", didIssuer, "-s", didSubject, "-t", "Europass", "-p", "LD_PROOF"))
    }

    "vc issue VerifiableAttestation LD_PROOF" {
        VcIssueCommand().parse(listOf("-i", didIssuer, "-s", didSubject, "-t", "VerifiableAttestation"))
    }

    "vc issue Europass JWT" {
        VcIssueCommand().parse(listOf("-i", didIssuer, "-s", didSubject, "-t", "Europass", "-p", "JWT"))
    }

    "vc issue VerifiableAttestation JWT" {
        VcIssueCommand().parse(listOf("-i", didIssuer, "-s", didSubject, "-t", "VerifiableAttestation", "-p", "JWT"))
    }
})
