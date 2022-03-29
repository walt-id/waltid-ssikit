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

    val keyService = KeyService.getService()
    // ServiceRegistry.registerService<KIeyService>(InMemoryKeyService())

    "1. key gen --help" {
        val e = shouldThrow<PrintHelpMessage> {
            GenKeyCommand().parse(listOf("--help"))
        }
        val message = e.command.getFormattedHelp()
        message shouldContain "-a, --algorithm [Ed25519|Secp256k1|RSA]"
    }

    "2. key gen Ed25519" {
        GenKeyCommand().parse(listOf("-a", "Ed25519"))
    }

    "3. key gen Secp256k1" {
        GenKeyCommand().parse(listOf("-a", "Secp256k1"))
    }

    "4. key gen RSA" {
        GenKeyCommand().parse(listOf("-a", "RSA"))
    }

    "5. key export Secp256k1" {
        val key = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
        ExportKeyCommand().parse(listOf(key.id))
    }

    "6. key export Ed25519" {
        val key = KeyService.getService().generate(KeyAlgorithm.EdDSA_Ed25519)
        ExportKeyCommand().parse(listOf(key.id))
    }

    "7. key import Ed25519 priv key JWK" {
        ImportKeyCommand().parse(listOf("src/test/resources/cli/privKeyEd25519Jwk.json"))
        keyService.delete("45674a4ac169f7f4716804393d20480138a")
    }

    "8. key import Ed25519 pub key JWK" {
        ImportKeyCommand().parse(listOf("src/test/resources/cli/pubKeyEd25519Jwk.json"))
        keyService.delete("12374a4ac169f7f4716804393d20480138a")
    }

    "9. key import RSA priv key PEM" {
        ImportKeyCommand().parse(listOf("src/test/resources/key/privkey.pem"))
    }

    "10. key import RSA pub key PEM" {
        ImportKeyCommand().parse(listOf("src/test/resources/key/pubkey.pem"))
    }

    "11. key import RSA priv key JWK" {
        ImportKeyCommand().parse(listOf("src/test/resources/key/privkey.jwk"))
    }

    "12. key import Secp256k1 priv key JWK" {
        ImportKeyCommand().parse(listOf("src/test/resources/key/privKeySecp256k1Jwk.json"))
        keyService.delete("ed51ec3a165f4af8bef8298d99b41de7")
    }

    "13. key import Secp256k1 pub key JWK" {
        ImportKeyCommand().parse(listOf("src/test/resources/key/pubKeySecp256k1Jwk.json"))
        keyService.delete("c96fe427cef847e6b2b9675cec31a2bb")
    }

    "14. clear keys" {
        keyService.listKeys().forEach { keyService.delete(it.keyId.toString()) }
    }
})
