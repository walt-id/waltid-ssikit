package org.letstrust.cli


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import org.letstrust.CliConfig
import org.letstrust.model.DidMethod
import org.letstrust.model.encodePretty
import org.letstrust.model.toDidUrl
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
    val didService = DidService
    val config: CliConfig by requireObject()
    val dest: File? by argument().file().optional()
    val method: String by option("-m", "--did-method", help = "Specify DID method [key]").choice(
        "key",
        "web",
        "ebsi"
    ).default("key")
    val keyAlias: String by option("-k", "--key", help = "Specific key (ID or alias)").default("default")

    override fun run() {

        echo("Registering did:${method} (key: ${keyAlias}) ...")

        val keyId = if (keyAlias =="default") null else keyAlias

        val did = didService.create(DidMethod.valueOf(method), keyId)

        echo("\nResults:\n")
        echo("DID created: $did")

        val didDoc = didService.resolve(did)

        if (didDoc == null) {
            echo("\nCould not resolve: $did")
        } else {
            val didDocEnc = didDoc.encodePretty()
            echo("\ndid document:\n$didDocEnc")

            dest?.let {
                echo("Saving DID to DEST file: ${it.absolutePath}")
                it.writeText(didDocEnc)
            }
        }
    }
}

class ResolveDidCommand : CliktCommand(
    name = "resolve",
    help = """Resolve DID.

        Constructs the DID Document."""
) {
    val did: String by option(help = "DID to be resolved").required()
    val config: CliConfig by requireObject()

    override fun run() {
        echo("Resolving $did ...")

        var encodedDid = when (did.contains("mattr")) {
            true -> DidService.resolveDidWeb(did.toDidUrl()).encodePretty()
            else -> DidService.resolve(did).encodePretty()
        }

        echo("\nResult:\n ${encodedDid}")

        val didFileName = "${did.replace(":", "-").replace(".", "_")}.json"
        val destFile = File(config.dataDir + "/did/resolved/" + didFileName)
        echo("Saving DID to file: ${destFile.absolutePath}")
        destFile.writeText(encodedDid)
    }
}

class ListDidsCommand : CliktCommand(
    name = "list",
    help = """List DIDs

        List all created DIDs."""
) {
    override fun run() {
        echo("List DIDs ...")

        DidService.listDids().forEach { it -> echo("- $it") }
    }
}
