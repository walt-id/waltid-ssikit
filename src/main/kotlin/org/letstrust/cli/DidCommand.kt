package org.letstrust.cli


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import org.letstrust.model.DidMethod
import org.letstrust.model.DidUrl
import org.letstrust.model.encodePretty
import org.letstrust.services.did.DidService
import java.io.File

class DidCommand : CliktCommand(
    help = """Decentralized Identifiers (DIDs).

        DID related operations, like registering, updating and deactivating DIDs.
        
        Supported DID methods are "key", "web" and "ebsi"""
) {

    override fun run() {

    }
}

class CreateDidCommand : CliktCommand(
    name = "create",
    help = """Create DID.

        Generates an asymmetric keypair and register the DID containing the public key.
        
        """
) {
    val config: CliConfig by requireObject()
    val dest: File? by argument().file().optional()
    val method: String by option("-m", "--did-method", help = "Specify DID method [key]").choice(
        "key",
        "web",
        "ebsi"
    ).default("key")
    val keyAlias: String by option("-k", "--key", help = "Specific key (ID or alias)").default("default")

    override fun run() {

        echo("Registering did:${method} (key: ${keyAlias})...")

        val keyId = if (keyAlias == "default") null else keyAlias

        val did = DidService.create(DidMethod.valueOf(method), keyId)

        echo("\nResults:\n")
        echo("DID created: $did")

        val encodedDid = loadDidHelper(did)
        echo("DID document (below, JSON):\n\n$encodedDid")

        dest?.let {
            echo("\nSaving DID to file: ${it.absolutePath}")
            it.writeText(encodedDid)
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
        true -> DidService.resolveDidEbsiRaw(did)
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

        echo("\nResult:\n")

        echo(encodedDid)

        val didFileName = "${did.replace(":", "-").replace(".", "_")}.json"
        val destFile = File(config.dataDir + "/did/resolved/" + didFileName)
        echo("\nSaving DID to file: ${destFile.absolutePath}")
        destFile.writeText(encodedDid)
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
