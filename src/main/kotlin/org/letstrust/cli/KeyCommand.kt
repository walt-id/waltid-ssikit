package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import org.letstrust.services.key.KeyManagementService


class KeyCommand : CliktCommand(
    help = """Key Management.

        Key management functions like generation, export/import, and deletion."""
) {
    // val repo: RepoConfig by requireObject()
    val algorithm: String by option(help = "Key algorithm [Ed25519]").default("Ed25519")

    override fun run() {
    }
}

class GenCommand : CliktCommand(
    help = """Generate keys.

        Generates an asymmetric keypair by the specified algorithm.
        
        """
) {

    // val keyAlias: String by option("--key-alias", "-a", help = "Specific key alias").prompt()
    val algorithm: String by option("-g", "--algorithm", help = "Key algorithm [Secp256k1, Ed25519]").choice(
        "Ed25519",
        "Secp256k1",
        "RSA"
    ).default("Ed25519")

    override fun run() {
        echo("Generating $algorithm key pair")
        val keyId = when (algorithm) {
            "Ed25519" -> KeyManagementService.generateEd25519KeyPairNimbus()
            "Secp256k1" -> KeyManagementService.generateSecp256k1KeyPairBitcoinj()
            "RSA" -> KeyManagementService.generateRsaKeyPair()
            else -> IllegalArgumentException("Algorithm not supported")
        }
        echo("Key \"$keyId\" generated")
    }
}

class ExportKeyCommand : CliktCommand(
    name = "export",
    help = """Export keys.

        Export key in JWK format."""
) {

    val keyId: String by option(help = "Key ID or key alias").required()

    override fun run() {
        echo("Exporting key $keyId")
        val jwk = KeyManagementService.export(keyId)
        println(jwk)
    }
}

class ListKeysCommand : CliktCommand(
    name = "list",
    help = """List keys.

        List all keys in the key store."""
) {

    override fun run() {
        echo("List keys ...")

        KeyManagementService.listKeys().forEach {
            println("- $it")
        }
    }
}
