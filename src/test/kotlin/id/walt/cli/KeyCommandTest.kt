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
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.VerifiableAttestation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain


class KeyCommandTest : StringSpec({

    ServiceMatrix("service-matrix.properties")

    val key = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)

    "key gen --help" {
        val e = shouldThrow<PrintHelpMessage> {
            GenKeyCommand().parse(listOf("--help"))
        }
        val message = e.command.getFormattedHelp()
        message shouldContain "-a, --algorithm [Ed25519|Secp256k1]"
    }

    "key gen Ed25519" {
        GenKeyCommand().parse(listOf("-a", "Ed25519"))
    }

    "key gen Secp256k1" {
        GenKeyCommand().parse(listOf("-a", "Secp256k1"))
    }
// TODO
//    "key import Secp256k1" {
//        ImportKeyCommand().parse(listOf("jwk.json"))
//    }
//
//    "key import Ed25519" {
//        ImportKeyCommand().parse(listOf("jwk.json"))
//    }
//
//    "key export Secp256k1" {
//        ExportKeyCommand().parse(listOf("key-id"))
//    }
//
//    "key export Ed25519" {
//        ExportKeyCommand().parse(listOf("key-id"))
//    }
})
