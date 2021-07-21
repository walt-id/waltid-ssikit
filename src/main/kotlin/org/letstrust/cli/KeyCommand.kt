package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import org.letstrust.services.CryptoProvider
import org.letstrust.common.readWhenContent
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.crypto.KeyId
import org.letstrust.services.key.KeyFormat
import org.letstrust.services.key.KeyService
import org.letstrust.services.keystore.KeyType
import java.io.File

private val keyService = KeyService.getService()

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
            "Ed25519" -> keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
            "Secp256k1" -> keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
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

    val keyFile: File by argument("JWK-FILE", help = "File containing the JWK key (e.g. jwk.json)").file()
    val provider: CryptoProvider by option(
        "-p",
        "--provider",
        help = "Crypto provider of the imported key"
    ).enum<CryptoProvider>().default(CryptoProvider.SUN)

    override fun run() {
        val keyStr = readWhenContent(keyFile)
        echo("Importing key: $keyStr")
        val keyId: KeyId = keyService.import(keyStr)

        echo("\nResults:\n")

        echo("Key \"${keyId.id}\" imported.")
    }
}

class ExportKeyCommand : CliktCommand(
    name = "export",
    help = """Export keys.

        Export key in JWK format."""
) {

    val keyId: String by argument("KEY-ID", help = "Key ID or key alias")
    val keyFormat: KeyFormat by option("-f", "--key-format", help = "Key format of exported key").enum<KeyFormat>()
        .default(KeyFormat.JWK)
    val exportPrivate by option("--priv", help = "Export public or private key").flag("--pub", default = false)

    override fun run() {
        val exportKeyType = if (!exportPrivate) KeyType.PUBLIC else KeyType.PRIVATE

        echo("Exporting $exportPrivate key \"$keyId\"...")
        val jwk = keyService.export(keyId, keyFormat, exportKeyType)

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

        keyService.listKeys().forEachIndexed { index, key ->
            echo("- ${index + 1}: \"${key.keyId}\" (Algorithm: \"${key.algorithm.name}\", provided by \"${key.cryptoProvider.name}\")")
        }
    }
}
