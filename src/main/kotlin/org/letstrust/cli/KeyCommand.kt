package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import org.letstrust.crypto.KeyAlgorithm
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

        Generates an asymmetric keypair by the specified algorithm. Supported algorithms are ECDSA Secp256k1 & EdDSA Ed25519 (default)
        
        """
) {

    // val keyAlias: String by option("--key-alias", "-k", help = "Specific key alias").prompt()
    val algorithm: String by option("-a", "--algorithm", help = "Key algorithm [Ed25519]").choice(
        "Ed25519",
        "Secp256k1"
    ).default("Ed25519")

    override fun run() {
        echo("Generating $algorithm key pair...")
        val keyId = when (algorithm) {
            "Ed25519" -> KeyManagementService.generate(KeyAlgorithm.EdDSA_Ed25519)
            "Secp256k1" -> KeyManagementService.generate(KeyAlgorithm.ECDSA_Secp256k1)
            // TODO add RSA: "RSA" -> KeyManagementService.generateRsaKeyPair()
            else -> throw IllegalArgumentException("Algorithm not supported")
        }
        echo("Key \"$keyId\" generated.")
    }
}

class ImportKeyCommand : CliktCommand(
    name = "import",
    help = """Import keys.

        Import key in JWK format."""
) {

    val keyId: String by option(help = "Key ID or key alias").required()

    override fun run() {
        echo("Importing key \"$keyId\"...")
        //val jwk = KeyManagementService.import ...

        println("\nResults:\n")

        println("todo")
    }
}

class ExportKeyCommand : CliktCommand(
    name = "export",
    help = """Export keys.

        Export key in JWK format."""
) {

    val keyId: String by option(help = "Key ID or key alias").required()

    override fun run() {
        echo("Exporting key \"$keyId\"...")
        val jwk = KeyManagementService.export(keyId)

        println("\nResults:\n")

        println(jwk)
    }
}

class ListKeysCommand : CliktCommand(
    name = "list",
    help = """List keys.

        List all keys in the key store."""
) {

    override fun run() {
        echo("Listing keys ...")

        echo("\nResults:\n")

        KeyManagementService.listKeys().forEachIndexed { index, key ->
            echo("- ${index + 1}: \"${key.keyId}\" (Algorithm: \"${key.algorithm.name}\", provided by \"${key.cryptoProvider.name}\")")
        }
    }
}
