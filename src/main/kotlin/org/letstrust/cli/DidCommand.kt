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
import org.letstrust.DidService
import org.letstrust.KeyManagementService
import org.letstrust.model.encodePretty
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

        Generates an asymetric keypair and register the DID containing the public key.
        
        """
) {
    val didService = DidService
    val config: CliConfig by requireObject()
    val dest: File? by argument().file().optional()
    val method: String by option("-m", "--did-method", help = "Specifiy DID method [key, web, ebsi]").choice(
        "key",
        "web",
        "ebsi"
    ).required()
    val keyAlias: String by option("-a", "--key-alias", help = "Specific key alias").default("default")

    override fun run() {

        echo("Registering did:${method} (key: ${keyAlias}) ...")

        var keys = KeyManagementService.loadKeys(keyAlias)

        val did = didService.createDid(method, keys)

        echo("\nResults:\n")
        echo("DID created: $did")

        val didDoc = didService.resolveDid(did)

        if (didDoc == null) {
            echo("\nCould not resolve: $did")
        } else {
            val destName = dest?.name ?: "${didDoc.id?.replace(":", "-")}.json"
            val destFile = File(config.dataDir + "/" + destName)
            echo("Saving DID to file: ${destFile.absolutePath}")
            destFile.createNewFile()
            destFile.writeText(didDoc.encodePretty())

            echo("\ndid document:\n" + didDoc.encodePretty())
        }

        if (config.verbose) {
            echo("Lets make some noise here ...")
        }
    }
}

class ResolveDidCommand : CliktCommand(
    name = "resolve",
    help = """Resolve DID.

        Constructs the DID Document."""
) {
    val did: String by option(help = "DID to be resolved").required()

    override fun run() {
        echo("Resolving $did ...")

        val did = DidService.resolveDid(did)

        echo("\nResult:\n ${did?.encodePretty()}")
    }
}

class ListDidsCommand : CliktCommand(
    name = "list",
    help = """List DIDs

        List all created DIDs."""
) {
    override fun run() {
        echo("List DIDs")

        DidService.listDids()?.forEach { it -> println(it) }
    }
}
