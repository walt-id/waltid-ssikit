package id.walt.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import id.walt.crypto.KeyAlgorithm
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain


class KeyCommandTest : StringSpec({

    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")

    "key gen --help" {
        val e = shouldThrow<PrintHelpMessage> {
            GenKeyCommand().parse(listOf("--help"))
        }
        val message = e.command.getFormattedHelp()
        message shouldContain "-a, --algorithm [Ed25519|Secp256k1|RSA]"
    }

    "key gen Ed25519" {
        GenKeyCommand().parse(listOf("-a", "Ed25519"))
    }

    "key gen Secp256k1" {
        GenKeyCommand().parse(listOf("-a", "Secp256k1"))
    }

    "key gen RSA" {
        GenKeyCommand().parse(listOf("-a", "RSA"))
    }

    "key export Secp256k1" {
        val key = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
        ExportKeyCommand().parse(listOf(key.id))
    }

    "key export Ed25519" {
        val key = KeyService.getService().generate(KeyAlgorithm.EdDSA_Ed25519)
        ExportKeyCommand().parse(listOf(key.id))
    }

    "key import Ed25519 priv key JWK" {
        ImportKeyCommand().parse(listOf("src/test/resources/cli/privKeyEd25519Jwk.json"))
        KeyService.getService().delete("45674a4ac169f7f4716804393d20480138a")
    }

    "key import Ed25519 pub key JWK" {
        ImportKeyCommand().parse(listOf("src/test/resources/cli/pubKeyEd25519Jwk.json"))
        KeyService.getService().delete("12374a4ac169f7f4716804393d20480138a")
    }

// Import in PEM format currently not supported
//    "key import Ed25519 priv key PEM" {
//        ImportKeyCommand().parse(listOf("src/test/resources/cli/privKeyEd25519Pem.txt", "src/test/resources/cli/pubKeyEd25519Pem.txt"))
//    }
//
//    "key import Ed25519 pub key PEM" {
//        ImportKeyCommand().parse(listOf("src/test/resources/cli/pubKeyEd25519Pem.txt"))
//    }
})
