package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import id.walt.cli.logic.KeyCommandLogic
import id.walt.common.readWhenContent
import id.walt.crypto.KeyId
import id.walt.services.key.KeyFormat
import id.walt.services.keystore.KeyType
import java.io.File

class KeyCommand : CliktCommand(
    help = """Key Management.

        Key management functions like generation, export/import, and deletion."""
) {
    val algorithm: String by option(help = "Key algorithm [Ed25519]").default("Ed25519")

    override fun run() {
    }
}

class GenKeyCommand : CliktCommand(
    name = "gen",
    help = """Generate keys.

        Generates an asymmetric keypair by the specified algorithm. Supported algorithms are ECDSA Secp256k1 & EdDSA Ed25519 (default)
        
        """
) {

    val algorithm: String by option("-a", "--algorithm", help = "Key algorithm [Ed25519]").choice(
        "Ed25519",
        "Secp256k1"
    ).default("Ed25519")

    override fun run() {
        echo("Generating $algorithm key pair...")

        val keyId = KeyCommandLogic.genKey(algorithm)

        echo("Key \"$keyId\" generated.")
    }
}

class ImportKeyCommand : CliktCommand(
    name = "import",
    help = """Import key in JWK format.

        Based on the JWK key ID and key material an internal key object will be
        created and placed in the corresponding key store."""
) {

    val keyFile: File by argument("JWK-FILE", help = "File containing the JWK key (e.g. jwk.json)").file()

    override fun run() {
        val keyStr = readWhenContent(keyFile)
        echo("Importing key: $keyStr")


        val keyId: KeyId = KeyCommandLogic.import(keyStr)

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

        echo("Exporting $exportKeyType key \"$keyId\"...")
        val jwk = KeyCommandLogic.export(keyId, keyFormat, exportKeyType)

        echo("\nResults:\n")

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

        KeyCommandLogic.listKeys().forEachIndexed { index, (keyId, algorithm, cryptoProvider) ->
            echo("- ${index + 1}: \"${keyId}\" (Algorithm: \"${algorithm.name}\", provided by \"${cryptoProvider.name}\")")
        }
    }
}
