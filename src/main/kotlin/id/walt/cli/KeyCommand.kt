package id.walt.cli


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import id.walt.common.readWhenContent
import id.walt.crypto.KeyAlgorithm
import id.walt.services.key.KeyFormat
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import java.nio.file.Path

private val keyService = KeyService.getService()

class KeyCommand : CliktCommand(
    help = """Key Management.

        Key management functions like generation, export/import, and deletion."""
) {
    val algorithm: String by option(help = "Key algorithm [Ed25519]").default("Ed25519")

    override fun run() {
    }
}

class GenKeyCommand : CliktCommand(
    name = "gen", help = """Generate keys.

        Generates an asymmetric keypair by the specified algorithm. Supported algorithms are ECDSA Secp256k1 & EdDSA Ed25519 (default)
        
        """
) {

    val algorithm: String by option("-a", "--algorithm", help = "Key algorithm [Ed25519]").choice(
        "Ed25519", "Secp256k1", "RSA"
    ).default("Ed25519")

    override fun run() {
        echo("Generating $algorithm key pair...")

        val keyId = keyService.generate(KeyAlgorithm.fromString(algorithm))

        echo("Key \"$keyId\" generated.")
    }
}

class ImportKeyCommand : CliktCommand(
    name = "import", help = """Import key in JWK or PEM format.

        For JWK Keys: Based on the JWK key ID and key material an internal key object will be
        created and placed in the corresponding key store.
        
        For PEM keys: If there's no key ID in the PEM file (which is usually the case), a random key ID
        will be generated for you and based on the key material an internal key object will be
        created and placed in the corresponding key store. PEM files must have the file extension 'pem'.
        """
) {

    val keyFile: Path by argument("file", help = "File containing the key (e.g. jwk.json or privkey.pem)").path()

    override fun run() {
        echo("Importing key from \"$keyFile\"...")

        val keyStr = readWhenContent(keyFile)

        val keyId = keyService.importKey(keyStr)

        echo("\nResults:\n")

        echo("Key \"${keyId.id}\" imported.")
    }
}

class ExportKeyCommand : CliktCommand(
    name = "export", help = """Export keys.

        Export key in JWK format."""
) {

    val keyId: String by argument("KEY-ID", help = "Key ID or key alias")
    val keyFormat: KeyFormat by option("-f", "--key-format", help = "Key format of exported key").enum<KeyFormat>()
        .default(KeyFormat.JWK)
    val exportPrivate by option("--priv", help = "Export public or private key").flag("--pub", default = false)

    override fun run() {
        val exportKeyType = if (!exportPrivate) KeyType.PUBLIC else KeyType.PRIVATE

        echo("Exporting $exportKeyType key \"$keyId\"...")
        val jwk = keyService.export(keyId, keyFormat, exportKeyType)

        echo("\nResults:\n")

        println(jwk)
    }
}

class ListKeysCommand : CliktCommand(
    name = "list", help = """List keys.

        List all keys in the key store."""
) {

    override fun run() {
        echo("Listing keys ...")

        echo("\nResults:\n")

        keyService.listKeys().forEachIndexed { index, (keyId, algorithm, cryptoProvider) ->
            echo("- ${index + 1}: \"${keyId}\" (Algorithm: \"${algorithm.name}\", provided by \"${cryptoProvider.name}\")")
        }
    }
}

class DeleteKeyCommand : CliktCommand(
    name = "delete", help = """Delete key.

        Deletes the key with the specified ID.
        """
) {

    val keyId: String by argument("KEY-ID", help = "Key ID or key alias")

    override fun run() {
        echo("Deleting key \"$keyId\"...")

        keyService.delete(keyId)

        echo("\nResults:\n")

        echo("Key \"${keyId}\" deleted.")
    }
}
