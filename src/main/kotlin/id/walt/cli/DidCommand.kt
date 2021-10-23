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
import com.github.ajalt.clikt.parameters.types.file
import id.walt.common.prettyPrint
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.model.encodePretty
import id.walt.services.did.DidService
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

        Creates a DID document based on the corresponding SSI ecosystem (DID method). 
        Optionally the associated asymmetric keypair is also created.
        
        """
) {
    val config: CliConfig by requireObject()
    val dest: File? by argument().file().optional()
    val method: String by option("-m", "--did-method", help = "Specify DID method [key]").choice(
        "key",
        "web",
        "ebsi"
    ).default("key")
    val keyAlias: String by option("-k", "--key", help = "Specific key (ID or alias)").default("new")

    override fun run() {

        echo("Creating did:${method} (key: ${keyAlias})")

        val keyId = if (keyAlias == "new") null else keyAlias

        val did = DidService.create(DidMethod.valueOf(method), keyId)

        echo("\nResults:\n")
        echo("DID created: $did")

        val encodedDid = loadDidHelper(did)
        echo("DID document (below, JSON):\n\n$encodedDid")

        //TODO replace the following with
        //Custodian.getService().storeDid(did, didDoc)

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
