package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import org.letstrust.KeyManagementService


class key : CliktCommand(
    help = """Key management.

        Key management functions like generation, export/import, and deletion."""
) {
    // val repo: RepoConfig by requireObject()
    val algorithm: String by option(help = "Key algorithm [Secp256k1, Ed25519]").default("Ed25519")

    override fun run() {
    }
}

class gen : CliktCommand(
    help = """Generate keys.

        Generates an asymetric keypair by the specified alogrithm.
        
        """
) {

    val keyAlias: String by option("--key-alias", "-a", help = "Specific key alias").prompt()
    val algorithm: String by option("--algorithm", "-g", help = "Key algorithm [Secp256k1, Ed25519]").choice(
        "Ed25519",
        "Secp256k1",
        "RSA"
    ).default("Ed25519")

    override fun run() {
        echo("Generating key with ${algorithm}")
    }
}

class exportKey : CliktCommand(
    name = "export",
    help = """Export keys.

        Export key in JWK format."""
) {

    val keyId: String by option(help = "Key ID or key alias").required()

    override fun run() {
        echo("Exporting key ${keyId}")
    }
}

class listKeys : CliktCommand(
    name = "list",
    help = """List keys

        List all keys in the key store."""
) {

    override fun run() {
        echo("List keys")

        KeyManagementService.listkeys().forEach {
            println("key $it")
        }
    }
}
