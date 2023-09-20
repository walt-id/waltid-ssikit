package id.walt.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import id.walt.crypto.KeyAlgorithm
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.string.shouldContain


class KeyCommandTest : StringSpec({

    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")

    val keyService = KeyService.getService()
    // ServiceRegistry.registerService<KIeyService>(InMemoryKeyService())

    "1. key gen --help" {
        val e = shouldThrow<PrintHelpMessage> {
            GenKeyCommand().parse(listOf("--help"))
        }
        val message = e.context?.command?.getFormattedHelp()
        message shouldContain "--algorithm"
        message shouldContain "Ed25519|Secp256k1|RSA|Secp256r1"
    }

    "2. key generate" {
        forAll(
            row("Ed25519"),
            row("Secp256k1"),
            row("RSA")
        ) { alg ->
            GenKeyCommand().parse(listOf("-a", alg))
        }
    }

    "3. key export" {
        forAll(
            row(KeyAlgorithm.ECDSA_Secp256k1),
            row(KeyAlgorithm.EdDSA_Ed25519),
            row(KeyAlgorithm.RSA)
        ) { alg ->
            val key = KeyService.getService().generate(alg)
            ExportKeyCommand().parse(listOf(key.id))
        }
    }

    "4. key import" {
        forAll(
//            Ed25519 priv key JWK
            row("src/test/resources/cli/privKeyEd25519Jwk.json", "45674a4ac169f7f4716804393d20480138a"),
//            Ed25519 pub key JWK
            row("src/test/resources/cli/pubKeyEd25519Jwk.json", "12374a4ac169f7f4716804393d20480138a"),
//            RSA key PEM
            row("src/test/resources/key/pem/rsa/rsa.pem", ""),
//            Secp256k1 key PEM
            row("src/test/resources/key/pem/ecdsa/secp256k1.pem", ""),
//            Ed25519 key PEM
            row("src/test/resources/key/pem/ed25519/ed25519.pem", ""),
//            RSA priv key JWK
            row("src/test/resources/key/privkey.jwk", ""),
//            Secp256k1 priv key JWK
            row("src/test/resources/key/privKeySecp256k1Jwk.json", "ed51ec3a165f4af8bef8298d99b41de7"),
//             Secp256k1 pub key JWK
            row("src/test/resources/key/pubKeySecp256k1Jwk.json", "c96fe427cef847e6b2b9675cec31a2bb"),
        ) { key, keyId ->
            ImportKeyCommand().parse(listOf(key))
            keyService.delete(keyId)
        }
    }

    "5. key delete" {
        val kid = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        DeleteKeyCommand().parse(listOf(kid.id))
        shouldThrow<Exception> { keyService.load(kid.id) }
    }

    "6. clear keys" {
        keyService.listKeys().forEach { keyService.delete(it.keyId.toString()) }
    }
})
