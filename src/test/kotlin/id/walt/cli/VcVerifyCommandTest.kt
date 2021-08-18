package id.walt.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import id.walt.servicematrix.ServiceMatrix
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import java.io.File

// TODO implement tests
class VcVerifyCommandTest : StringSpec({

    ServiceMatrix("service-matrix.properties")

    "vc verify --help" {
        val e = shouldThrow<PrintHelpMessage> {
            VerifyVcCommand().parse(listOf("--help"))
        }
        val message = e.command.getFormattedHelp()
        message shouldContain "Verify VC or VP"
    }

    "vc verify default" {
        //val file = File(javaClass.classLoader.getResource("verifiable-credentials/vp-input.json").file)
        //VerifyVcCommand().parse(listOf(file.absolutePath))
    }

    "vc verify Europass LD_PROOF" {

    }

    "vc verify VerifiableAttestation LD_PROOF" {

    }

    "vc verify Europass JWT" {

    }

    "vc verify VerifiableAttestation JWT" {

    }

    "vc verify policies list" {

    }
})
