package id.walt.cli


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import id.walt.common.prettyPrint
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.services.crypto.CryptoService
import id.walt.services.did.DidService
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

class DidCommand : CliktCommand(
    help = """Decentralized Identifiers (DIDs).

        DID related operations, like registering, updating and deactivating DIDs.
        
        Supported DID methods are "key", "web" and "ebsi"""
) {
    override fun run() {}
}

class CreateDidCommand : CliktCommand(
    name = "create",
    help = """Create DID.

        Creates a DID document based on the corresponding SSI ecosystem (DID method). 
        Optionally the associated asymmetric key is also created.

        """
) {
    val config: CliConfig by requireObject()
    val method: String by option("-m", "--did-method", help = "Specify DID method [key]")
        .choice("key", "web", "ebsi").default("key")

    val keyAlias: String? by option("-k", "--key", help = "Specific key (ID or alias)")
    val domain: String by option("-d", "--domain", help = "Domain for did:web").default("walt.id")
    val path: String? by option("-p", "--path", help = "Path for did:web")
    val dest: Path? by argument("destination-file").path().optional()

    override fun run() {

        val keyId = keyAlias ?: when (method) {
            "ebsi" -> CryptoService.getService().generateKey(KeyAlgorithm.ECDSA_Secp256k1).id
            else -> CryptoService.getService().generateKey(KeyAlgorithm.EdDSA_Ed25519).id
        }

        echo("Creating did:${method} (key: ${keyId})")

        val did = DidService.create(DidMethod.valueOf(method), keyId, DidService.DidWebOptions(domain, path))

        echo("\nResults:\n")
        echo("\nDID created: $did\n")

        val encodedDid = loadDidHelper(did)
        echo("\nDID document (below, JSON):\n\n$encodedDid")

        dest?.let {
            echo("\nSaving DID to file: ${dest!!.absolutePathString()}")
            dest!!.writeText(encodedDid)
        }

        when (method) {
            "web" -> echo(
                "\nInstall this did:web at: https://$domain/${
                    path?.replace(":", "/")?.plus("/") ?: ""
                }.well-known/did.json"
            )
        }
    }
}

fun loadDidHelper(did: String) = when {
    did.contains("web") -> DidService.resolveDidWebDummy(DidUrl.from(did)).encodePretty()
    did.contains("ebsi") -> DidService.loadDidEbsi(did).encodePretty()
    else -> DidService.load(did).encodePretty()
}

fun resolveDidHelper(did: String, raw: Boolean) = when {
    did.contains("web") -> DidService.resolveDidWebDummy(DidUrl.from(did)).encodePretty()
    did.contains("ebsi") -> when (raw) {
        true -> DidService.resolveDidEbsiRaw(did).prettyPrint()
        else -> DidService.resolveDidEbsi(did).encodePretty()
    }
    else -> DidService.resolve(did).encodePretty()
}

class ResolveDidCommand : CliktCommand(
    name = "resolve",
    help = """Resolve DID.

        Resolves the DID document. Use option RAW to disable type checking."""
) {
    val did: String by option("-d", "--did", help = "DID to be resolved").required()
    val raw by option("--raw", "-r").flag("--typed", "-t", default = false)
    val config: CliConfig by requireObject()

    override fun run() {
        echo("Resolving DID \"$did\"...")

        val encodedDid = resolveDidHelper(did, raw)

        echo("\nResults:\n")
        echo("DID resolved: \"$did\"")
        echo("DID document (below, JSON):\n")

        echo(encodedDid)

        val didFileName = "${did.replace(":", "-").replace(".", "_")}.json"
        val destFile = File(config.dataDir + "/did/resolved/" + didFileName)
        destFile.writeText(encodedDid)
        echo("\nDID document was saved to file: ${destFile.absolutePath}")
    }
}

class ListDidsCommand : CliktCommand(
    name = "list",
    help = """List DIDs

        List all created DIDs."""
) {
    override fun run() {
        echo("Listing DIDs...")

        echo("\nResults:\n")

        DidService.listDids().forEachIndexed { index, did -> echo("- ${index + 1}: $did") }
    }
}

class ImportDidCommand : CliktCommand(
    name = "import",
    help = "Import DID to custodian store"
) {

    val did: String by argument()

    override fun run() {
        DidService.importDidAndKey(did)
    }
}

